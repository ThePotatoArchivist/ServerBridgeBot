@file:JvmName("Main")

package archives.tater.bot.bridge

import archives.tater.bot.bridge.SaveData.Companion.update
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.execute
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.messageFlags
import io.github.cdimascio.dotenv.Dotenv
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
                bridgeCommand.id -> connectBridge(interaction, Snowflake(interaction.command.strings["channel"]!!.toLong()))
                unbridgeCommand.id -> disconnectBridge(interaction)
            }
        }

        on<MessageCreateEvent> {
            if (member?.isSelf == true || message.webhookId != null) return@on

            saveData.value[message.channel]?.apply {
                messages[message] = channels.mapNotNull { (channel, webhook) ->
                    if (channel.id == message.channelId) return@mapNotNull null

                    webhook.value.execute(webhook.value.token!!) {
                        avatarUrl = member?.avatar?.cdnUrl?.toUrl()
                        username = member?.effectiveName
                        content = message.content.takeUnless { it.isBlank() } ?: "*ERROR: No content*"
                    }
                }
            }
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

suspend fun connectBridge(interaction: GuildChatInputCommandInteraction, otherChannelId: Snowflake) {
    val response = interaction.deferPublicResponse()

    if (saveData.value[interaction.channel] != null) {
        response.respond {
            content = "Cannot connect to multiple channels"
        }
        return
    }

    if (saveData.value[otherChannelId] != null) {
        response.respond {
            content = "That channel is already connected"
        }
        return
    }

    val otherChannel = interaction.kord.getChannelOf<GuildMessageChannel>(otherChannelId) ?: run {
        response.respond {
            content = "Could not find or access channel `${otherChannelId}`"
        }
        return
    }

    saveData.update {
        it.add(
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
        it.remove(connection)
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
