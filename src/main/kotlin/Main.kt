@file:JvmName("Main")

package archives.tater.bot.bridge

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.string
import io.github.cdimascio.dotenv.Dotenv

val dotenv: Dotenv = Dotenv.load()

val saveData = SaveData()

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    with (Kord(dotenv["BOT_TOKEN"])) {

        val bridgeCommand = createGlobalChatInputCommand("bridge", "Bridge one or more discord channels") {
            string("channel", "Channel to bridge to. The bot must have access to this channel.") {
                required = true
            }
        }

        on<ChatInputCommandInteractionCreateEvent> {
            if (interaction.invokedCommandId != bridgeCommand.id) return@on

            interaction.respondEphemeral {
                content = "${getChannel(Snowflake(interaction.command.strings["channel"]!!.toLong()))}"
            }
        }

        on<ReadyEvent> {
            println("Logged in!")
        }

        login {
            intents += Intent.MessageContent
        }
    }
}