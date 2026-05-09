package archives.tater.bot.bridge

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

inline fun <E> MutableCollection<E>.removeMatching(filter: (E) -> Boolean) {
    iterator().apply {
        forEach {
            if (filter(it))
                remove()
        }
    }
}

abstract class MappedSerializer<T, U>(val delegate: KSerializer<U>) : KSerializer<T> {
    override val descriptor: SerialDescriptor get() = delegate.descriptor

    abstract fun decompose(value: T): U
    abstract fun construct(value: U): T

    override fun serialize(encoder: Encoder, value: T) {
        delegate.serialize(encoder, decompose(value))
    }

    override fun deserialize(decoder: Decoder): T = construct(delegate.deserialize(decoder))
}
