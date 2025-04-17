package io.zenwave360.zdl.antlr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class FluentMap extends LinkedHashMap<String, Object> {

    public FluentMap with(String key, Object value) {
        put(key, value);
        return this;
    }

    public FluentMap appendTo(String collection, String key, Object value) {
        if(!containsKey(collection)) {
            put(collection, new FluentMap());
        }
        ((Map) get(collection)).put(key, value);
        return this;
    }

    public FluentMap appendTo(String collection, Map value) {
        if(!containsKey(collection)) {
            put(collection, new FluentMap());
        }
        ((Map) get(collection)).putAll(value);
        return this;
    }

    public FluentMap appendToList(String collection, Object value) {
        if(!containsKey(collection)) {
            put(collection, new ArrayList());
        }
        ((List) get(collection)).add(value);
        return this;
    }
}
