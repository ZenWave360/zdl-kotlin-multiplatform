package io.zenwave360.zdl.antlr

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException

actual object JSONPath {

    private val conf: Configuration = Configuration
        .builder()
        .options(setOf())
        .build()

    private fun unwrap(source: Any?): Any? = when (source) {
        is FluentMap -> source.asJavaMap()
        is Map<*, *> -> source
        else -> source
    }

    @Suppress("UNCHECKED_CAST")
    actual fun <T> get(source: Any?, path: String, defaultValue: T?): T? {
        val unwrapped = unwrap(source)
        return try {
            JsonPath.using(conf).parse(unwrapped).read<Any?>(path) as? T ?: defaultValue
        } catch (_: PathNotFoundException) {
            defaultValue
        } catch (_: Exception) {
            defaultValue
        }
    }

    actual fun get(source: Any?, path: String): Any? {
        val unwrapped = unwrap(source)
        return try {
            JsonPath.using(conf).parse(unwrapped).read<Any?>(path)
        } catch (_: PathNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}

