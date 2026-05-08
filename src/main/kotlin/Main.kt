@file:JvmName("Main")

package archives.tater.bot.bridge

import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.github.cdimascio.dotenv.Dotenv

val dotenv: Dotenv = Dotenv.load()

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    with (Kord(dotenv["BOT_TOKEN"])) {

        on<ReadyEvent> {
            println("Logged in!")
        }

        login {
            intents += Intent.MessageContent
        }
    }
}