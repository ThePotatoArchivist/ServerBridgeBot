package archives.tater.bot.bridge

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.WebhookBehavior
import dev.kord.core.behavior.channel.ChannelBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Webhook
import dev.kord.core.entity.channel.MessageChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ChannelConnection(
    val channel: MessageChannelRef,
    val webhook: WebhookRef,
    @Transient val messages: MutableMap<Snowflake, MessageBehavior> = mutableMapOf()
) : Validatable {
    constructor(channel: MessageChannelBehavior, webhook: Webhook)
            : this(MessageChannelRef(channel), WebhookRef(webhook))

    context(kord: Kord)
    override suspend fun validate(): Boolean = channel.validate() && webhook.validate()

    fun putMessages(original: MessageBehavior, bridged: MessageBehavior) {
        messages[original.id] = bridged
        messages[bridged.id] = original
    }

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

    @Transient
    private lateinit var connectionsByChannel: MutableMap<Snowflake, BridgeConnection>

    context(kord: Kord)
    override suspend fun validate(): Boolean {
        connections.removeMatching { !it.validate() }
        connectionsByChannel = connections
            .flatMap { it.channels.map { (channel) -> channel.value.id to it } }
            .associate { it }
            .toMutableMap()
        return true
    }

    fun add(connection: BridgeConnection) {
        // TODO check allowed
        connections.add(connection)
        for ((channel) in connection.channels)
            connectionsByChannel[channel.value.id] = connection
    }

    fun remove(connection: BridgeConnection) {
        connections.remove(connection)
        for ((channel) in connection.channels)
            connectionsByChannel.remove(channel.value.id)
    }

    operator fun get(channel: ChannelBehavior): BridgeConnection? = get(channel.id)

    operator fun get(channelId: Snowflake): BridgeConnection? = connectionsByChannel[channelId]

}