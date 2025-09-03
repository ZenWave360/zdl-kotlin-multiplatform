package io.zenwave360.zdl.antlr

class FluentMap private constructor(
    private val backingMap: MutableMap<String, Any?>
) : MutableMap<String, Any?> by backingMap {

    companion object {
        fun build(block: FluentMap.() -> Unit = {}): FluentMap =
            FluentMap(mutableMapOf()).apply(block)
    }

    // Provide access to underlying Java Map (useful for JSONPath on JVM)
    fun asJavaMap(): MutableMap<String, Any?> = backingMap

    // Idiomatic API (avoid signature clash with MutableMap.put)
    fun putEntry(key: String, value: Any?): FluentMap = apply { backingMap[key] = value }
    fun putAllEntries(map: Map<String, Any?>): FluentMap = apply { backingMap.putAll(map) }

    fun appendToMap(collection: String, key: String, value: Any?): FluentMap = apply {
        val nestedMap = backingMap.getOrPut(collection) { mutableMapOf<String, Any?>() } as MutableMap<String, Any?>
        nestedMap[key] = value
    }

    fun appendToMap(collection: String, map: Map<String, Any?>): FluentMap = apply {
        val nestedMap = backingMap.getOrPut(collection) { mutableMapOf<String, Any?>() } as MutableMap<String, Any?>
        nestedMap.putAll(map)
    }

    fun appendToList(collection: String, value: Any?): FluentMap = apply {
        val nestedList = backingMap.getOrPut(collection) { mutableListOf<Any?>() } as MutableList<Any?>
        nestedList.add(value)
    }

    // Compatibility API (to be refactored later)
    fun with(key: String, value: Any?): FluentMap = putEntry(key, value)
    fun appendTo(collection: String, key: String, value: Any?): FluentMap = appendToMap(collection, key, value)
    fun appendTo(collection: String, map: Map<String, Any?>): FluentMap = appendToMap(collection, map)
}

