package io.zenwave360.zdl.antlr

object Inflector {

    fun pluralize(word: String?): String? {
        if (word == null) return null
        val w = word.trim()
        if (w.isEmpty()) return w
        // very small set of rules to match the Java version's behavior in common cases
        if (w.endsWith("s") || w.endsWith("x") || w.endsWith("z") || w.endsWith("ch") || w.endsWith("sh")) return w + "es"
        if (w.endsWith("y") && w.length > 1 && !isVowel(w[w.length - 2])) return w.dropLast(1) + "ies"
        return w + "s"
    }

    fun upperCamelCase(input: String?, vararg delimiters: Char): String? = camelCase(input, true, *delimiters)
    fun lowerCamelCase(input: String?, vararg delimiters: Char): String? = camelCase(input, false, *delimiters)

    fun camelCase(input: String?, uppercaseFirst: Boolean, vararg delimiters: Char): String? {
        if (input == null) return null
        var s = input.trim()
        if (s.isEmpty()) return s
        // normalize delimiters to underscore
        if (delimiters.isNotEmpty()) {
            delimiters.forEach { s = s.replace(it, '_') }
        }
        // split by underscores and non-alphanumeric boundaries
        val parts = s.split(Regex("[_\\s.-]+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return ""
        val first = parts.first()
        val rest = parts.drop(1).map { it.replaceFirstChar { c -> c.uppercaseChar() } }
        return if (uppercaseFirst) {
            (listOf(first.replaceFirstChar { it.uppercaseChar() }) + rest).joinToString("")
        } else {
            (listOf(first.replaceFirstChar { it.lowercaseChar() }) + rest).joinToString("")
        }
    }

    fun underscore(input: String?, vararg delimiters: Char): String? {
        if (input == null) return null
        var result = input.trim()
        if (result.isEmpty()) return result
        // handle camel case boundaries
        result = result.replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1_$2")
            .replace(Regex("([a-z\\d])([A-Z])"), "$1_$2")
            .replace('-', '_')
        if (delimiters.isNotEmpty()) {
            delimiters.forEach { result = result.replace(it, '_') }
        }
        return result.lowercase()
    }

    fun kebabCase(input: String?, vararg delimiters: Char): String? = underscore(input, *delimiters)?.replace('_', '-')

    fun capitalize(words: String?): String? {
        if (words == null) return null
        val result = words.trim()
        if (result.isEmpty()) return result
        return result.replaceFirstChar { it.uppercaseChar() }
    }

    private fun isVowel(c: Char): Boolean = c.lowercaseChar() in listOf('a', 'e', 'i', 'o', 'u')
}

