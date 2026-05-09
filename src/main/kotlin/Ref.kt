package archives.tater.bot.bridge

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.WebhookBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.KordEntity
import dev.kord.core.entity.channel.MessageChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

interface Validatable {
    context(kord: Kord)
    suspend fun validate(): Boolean
}

abstract class Ref<T: KordEntity> : Validatable {
    abstract val id: Snowflake

    @Transient
    lateinit var value: T
        protected set

    protected abstract suspend fun Kord.get(): T?

    context(kord: Kord)
    override suspend fun validate(): Boolean = kord.get()
        ?.also { value = it }
        .let { it != null }

    abstract class Serializer<T: Ref<*>>(val construct: (Snowflake) -> T) : MappedSerializer<T, Snowflake>(Snowflake.serializer()) {
        override fun decompose(value: T): Snowflake = value.id
        override fun construct(value: Snowflake): T = construct.invoke(value)
    }
}

@Serializable(with = MessageChannelRef.Serializer::class)
data class MessageChannelRef(override val id: Snowflake) : Ref<MessageChannelBehavior>() {
    constructor(channel: MessageChannelBehavior) : this(channel.id) { value = channel }

    override suspend fun Kord.get(): MessageChannelBehavior? = getChannelOf<MessageChannel>(id)

    object Serializer : Ref.Serializer<MessageChannelRef>(::MessageChannelRef)

}

@Serializable(with = WebhookRef.Serializer::class)
data class WebhookRef(override val id: Snowflake) : Ref<WebhookBehavior>() {
    constructor(webhook: WebhookBehavior) : this(webhook.id) { value = webhook }

    override suspend fun Kord.get(): WebhookBehavior? = getWebhookOrNull(id)

    object Serializer : Ref.Serializer<WebhookRef>(::WebhookRef)

}