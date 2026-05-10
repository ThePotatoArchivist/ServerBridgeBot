@file:JvmName("Main")

package archives.tater.bot.bridge

import archives.tater.bot.bridge.SaveData.Companion.update
import com.sun.org.apache.xml.internal.serializer.utils.Utils.messages
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.channel.ChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.execute
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.messageFlags
import io.github.cdimascio.dotenv.Dotenv
import jdk.internal.joptsimple.internal.Messages.message
import kotlinx.serialization.json.JsonNull.content
import kotlin.apply
import kotlin.io.path.Path

val dotenv: Dotenv = Dotenv.load()

val saveData = SaveData.load(Path("connections.json"), ::BridgeConnections)

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    with (Kord(dotenv["BOT_TOKEN"])) {

        val bridgeCommand = createGlobalChatInputCommand("bridge", "Bridge one or more discord channels") {
            string("channel", "Channel to bridge to. The bot must have access to this channel.") {
                required = true
            }
            dmPermission = false
        }
        val unbridgeCommand = createGlobalChatInputCommand("unbridge", "Disconnect bridge") {
            dmPermission = false
        }

        on<GuildChatInputCommandInteractionCreateEvent> {
            when (interaction.invokedCommandId) {
                bridgeCommand.id -> connectBridge(interaction, interaction.command.strings["channel"]!!)
                unbridgeCommand.id -> disconnectBridge(interaction)
            }
        }

        on<MessageCreateEvent> {
            bridgeMessageCreate(message, member)
        }

        on<MessageUpdateEvent> {
            bridgeMessageUpdate(message)
        }

        on<MessageDeleteEvent> {
            bridgeMessageDelete(channel, messageId)
        }

        on<ReadyEvent> {
            println("Logged in!")
            saveData.value.validate()
        }

        login {
            intents += Intent.MessageContent
        }
    }
}

private fun getContent(message: Message): String = message.content + message.attachments.joinToString(separator = "") {
    "\n" + it.url
}

suspend fun bridgeMessageCreate(message: Message, member: Member?) {
    if (member?.isSelf == true || message.webhookId != null) return

    saveData.value[message.channel]?.apply {
        for ((channel, webhook, messages) in channels) {
            if (channel.id == message.channelId) continue

            messages[message.id] = webhook.value.execute(webhook.value.token!!) {
                avatarUrl = member?.avatar?.cdnUrl?.toUrl()
                username = member?.effectiveName
                content = getContent(message)
            }
        }
    }
}

suspend fun bridgeMessageUpdate(message: MessageBehavior) {
    val bridgeConnection = saveData.value[message.channel] ?: return
    val newContent = getContent(message.asMessage())
    for ((webhook, messages) in bridgeConnection.channels)
        messages[message.id]?.edit(webhook.id, webhook.value.token!!) {
            content = newContent
        }
}

suspend fun bridgeMessageDelete(channel: ChannelBehavior, messageId: Snowflake) {
    val bridgeConnection = saveData.value[channel] ?: return
    for ((webhook, messages) in bridgeConnection.channels)
        messages[messageId]?.delete(webhook.id, webhook.value.token!!)
}

suspend fun connectBridge(interaction: GuildChatInputCommandInteraction, otherChannelId: String) {
    val otherChannelId = try { Snowflake(otherChannelId) } catch (_: NumberFormatException) {
        interaction.respondEphemeral {
            content = "Invalid channel id `${otherChannelId}`"
        }
        return
    }

    if (interaction.channelId == otherChannelId) {
        interaction.respondEphemeral {
            content = "Cannot connect channel to itself"
        }
        return
    }

    if (saveData.value[interaction.channel] != null) {
        interaction.respondEphemeral {
            content = "Cannot connect to multiple channels"
        }
        return
    }

    if (saveData.value[otherChannelId] != null) {
        interaction.respondEphemeral {
            content = "That channel is already connected to another channel"
        }
        return
    }

    val response = interaction.deferPublicResponse()

    val otherChannel = interaction.kord.getChannelOf<GuildMessageChannel>(otherChannelId) ?: run {
        response.respond {
            content = "Could not find or access channel `${otherChannelId}`"
        }
        return
    }

    saveData.update {
        add(
            BridgeConnection(
                ChannelConnection(interaction.channel, getOrCreateWebhookFor(interaction.channel)!!),
                ChannelConnection(otherChannel, getOrCreateWebhookFor(otherChannel)!!)
            )
        )
    }

    response.respond {
        content = "Connected to ${otherChannel.mention}"
    }
    otherChannel.createMessage {
        content = "${interaction.user.mention}: Connected to ${interaction.channel.mention}"
        messageFlags {
            +MessageFlag.SuppressNotifications
        }
    }
}

suspend fun disconnectBridge(interaction: GuildChatInputCommandInteraction) {
    val connection = saveData.value[interaction.channel] ?: run {
        interaction.respondEphemeral {
            content = "Not connected to any channel"
        }
        return
    }

    val response = interaction.deferPublicResponse()

    saveData.update {
        remove(connection)
    }

    response.respond {
        content = "Disconnected"
    }
    connection.channels.forEach { (channel) ->
        if (channel.id != interaction.channelId)
            channel.value.createMessage {
                content = "Disconnected"
            }
    }
}
