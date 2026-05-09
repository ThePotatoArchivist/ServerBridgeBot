package archives.tater.bot.bridge

import dev.kord.core.Kord
import dev.kord.core.behavior.WebhookBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import kotlinx.serialization.Serializable

@Serializable
data class ChannelConnection(val channel: MessageChannelRef, val webhook: WebhookRef) : Validatable {
    constructor(channel: MessageChannelBehavior, webhook: WebhookBehavior)
            : this(MessageChannelRef(channel), WebhookRef(webhook))

    context(kord: Kord)
    override suspend fun validate(): Boolean = channel.validate() && webhook.validate()

}

@Serializable
data class BridgeConnection(val channels: MutableSet<ChannelConnection>) : Validatable {
    constructor(vararg channels: ChannelConnection) : this(channels.toMutableSet())

    context(kord: Kord)
    override suspend fun validate(): Boolean {
        channels.removeMatching {
            !it.validate()
        }
        return channels.size > 1
    }

}

@Serializable
data class BridgeConnections(private val connections: MutableList<BridgeConnection> = mutableListOf()) : Validatable {
    context(kord: Kord)
    override suspend fun validate(): Boolean {
        connections.removeMatching { !it.validate() }
        return true
    }

    private val connectionsByChannel = connections
        .flatMap { it.channels.map { [channel] -> channel to it } }
        .associate { it }
        .toMutableMap()

    fun add(connection: BridgeConnection) {
        // TODO check allowed
        connections.add(connection)
        for ([channel] in connection.channels)
            connectionsByChannel[channel] = connection
    }

}