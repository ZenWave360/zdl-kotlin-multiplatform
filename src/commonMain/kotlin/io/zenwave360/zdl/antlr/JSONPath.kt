package io.zenwave360.zdl.antlr

expect object JSONPath {
    fun get(source: Any?, path: String): Any?

    fun <T> get(source: Any?, path: String, defaultValue: T?): T?
}

