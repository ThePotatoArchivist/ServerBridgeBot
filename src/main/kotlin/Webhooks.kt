package archives.tater.bot.bridge

import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.TopGuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createWebhook
import dev.kord.core.behavior.channel.threads.ThreadChannelBehavior
import dev.kord.core.entity.Webhook
import kotlinx.coroutines.flow.firstOrNull

const val WEBHOOK_NAME = "Server Bridge"

suspend fun topChannelOf(channelBehavior: GuildMessageChannelBehavior): TopGuildMessageChannelBehavior? = when (val channel = channelBehavior.asChannel()) {
    is TopGuildMessageChannelBehavior -> channel
    is ThreadChannelBehavior -> channel.parent as? TopGuildMessageChannelBehavior
    else -> null
}

suspend fun getWebhookFor(channelBehavior: GuildMessageChannelBehavior): Webhook? {
    return topChannelOf(channelBehavior)?.webhooks?.firstOrNull {
        it.data.applicationId == channelBehavior.kord.selfId
    }
}

suspend fun getOrCreateWebhookFor(channelBehavior: GuildMessageChannelBehavior): Webhook? {
    return getWebhookFor(channelBehavior) ?: topChannelOf(channelBehavior)?.createWebhook("Server Bridge") {
        reason = "To display bridged messages"
    }
}
