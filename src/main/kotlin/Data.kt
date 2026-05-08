package archives.tater.bot.bridge

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.WebhookBehavior
import dev.kord.core.behavior.channel.TextChannelBehavior
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.exception.EntityNotFoundException
import kotlinx.serialization.Serializable

@Serializable
data class ChannelConnection(val channelId: Snowflake, val webhookId: Snowflake) {
    lateinit var channel: TextChannelBehavior
    lateinit var webhook: WebhookBehavior

    context(kord: Kord)
    suspend fun getChannel(): TextChannelBehavior {
        if (::channel.isInitialized) return channel
        return (kord.getChannel(channelId) as? TextChannel)?.also {
            channel = it
        } ?: EntityNotFoundException.channelNotFound<TextChannel>(channelId)
    }

    context(kord: Kord)
    suspend fun getWebhook(): WebhookBehavior {
        if (::webhook.isInitialized) return webhook
        return kord.getWebhook(webhookId).also {
            webhook = it
        }
    }
}

@Serializable
data class BridgeConnection(val channels: Set<ChannelConnection>)

@Serializable
data class SaveData(private val connections: MutableList<BridgeConnection> = mutableListOf()) {
    private val connectionsByChannel = connections
        .flatMap { it.channels.map { [channelId] -> channelId to it } }
        .associate { it }
        .toMutableMap()

    fun add(connection: BridgeConnection) {
        // TODO check allowed
        connections.add(connection)
        for ([channelId] in connection.channels)
            connectionsByChannel[channelId] = connection
    }

}