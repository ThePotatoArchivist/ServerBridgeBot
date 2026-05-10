package archives.tater.bot.bridge

// probably pretty janky
class BiMap<K: Any, V: Any> private constructor(private val delegate: MutableMap<K, V> = mutableMapOf(), opposite: BiMap<V, K>? = null) : MutableMap<K, V> by delegate {
    val opposite: BiMap<V, K> = opposite ?: BiMap(opposite = this)

    constructor() : this(mutableMapOf())

    override fun put(key: K, value: V): V? {
        opposite.delegate.put(value, key)?.let {
            delegate.remove(it)
        }
        return delegate.put(key, value)
    }

    override fun remove(key: K): V? = delegate.remove(key)?.also {
        opposite.delegate.remove(it, key)
    }


    override fun clear() {
        delegate.clear()
        opposite.delegate.clear()
    }
}