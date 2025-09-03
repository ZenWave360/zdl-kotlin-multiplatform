package io.zenwave360.zdl.antlr

actual object JSONPath {

    @Suppress("UNCHECKED_CAST")
    actual fun <T> get(source: Any?, path: String, defaultValue: T?): T? =
        (get(source, path) as? T) ?: defaultValue

    actual fun get(source: Any?, path: String): Any? {
        if (source == null || path.isEmpty()) return null
        return try {
            evaluate(source, path)
        } catch (_: Throwable) {
            null
        }
    }

    private fun evaluate(source: Any?, path: String): Any? {
        var current: Any? = source
        var p = path.trim()
        if (p.startsWith("$")) p = p.removePrefix("$")
        if (p.startsWith(".")) p = p.removePrefix(".")
        if (p.isEmpty()) return current

        val tokens = tokenize(p)
        for (t in tokens) {
            current = when (t) {
                is PropToken -> getProp(current, t.name)
                is IndexToken -> getIndex(current, t.index)
                is WildcardToken -> current
            }
            if (current == null) return null
        }
        return current
    }

    private fun getProp(obj: Any?, name: String): Any? = when (obj) {
        is Map<*, *> -> obj[name]
        else -> try { obj.asDynamic()[name] } catch (_: Throwable) { null }
    }

    private fun getIndex(obj: Any?, index: Int): Any? = when (obj) {
        is List<*> -> obj.getOrNull(index)
        is Array<*> -> obj.getOrNull(index)
        else -> null
    }

    private sealed interface Token
    private data class PropToken(val name: String) : Token
    private data class IndexToken(val index: Int) : Token
    private data object WildcardToken : Token

    private fun tokenize(path: String): List<Token> {
        val out = mutableListOf<Token>()
        var i = 0
        fun readName(): String {
            val start = i
            while (i < path.length && (path[i].isLetterOrDigit() || path[i] == '_' )) i++
            val name = path.substring(start, i)
            return name
        }
        while (i < path.length) {
            when (path[i]) {
                '.' -> { i++; val name = readName(); if (name.isNotEmpty()) out += PropToken(name) }
                '[' -> {
                    i++
                    if (i < path.length && (path[i] == '\'' || path[i] == '"')) {
                        val quote = path[i++]
                        val start = i
                        while (i < path.length && path[i] != quote) i++
                        val name = path.substring(start, i)
                        out += PropToken(name)
                        if (i < path.length && path[i] == quote) i++
                        if (i < path.length && path[i] == ']') i++
                    } else if (i < path.length && path[i] == '*') {
                        out += WildcardToken
                        i++
                        if (i < path.length && path[i] == ']') i++
                    } else {
                        val start = i
                        while (i < path.length && path[i].isDigit()) i++
                        val idx = path.substring(start, i).toIntOrNull() ?: return out
                        out += IndexToken(idx)
                        if (i < path.length && path[i] == ']') i++
                    }
                }
                else -> {
                    val name = readName()
                    if (name.isNotEmpty()) out += PropToken(name)
                }
            }
        }
        return out
    }
}

