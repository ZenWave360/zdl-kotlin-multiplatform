package io.zenwave360.zdl.antlr

expect object JSONPath {
    fun <T> get(source: Any?, path: String): T?

    fun <T> get(source: Any?, path: String, defaultValue: T?): T?
}

