package io.github.zenwave360.zdl;

import java.util.LinkedHashMap;
import java.util.Map;

public class FluentMap extends LinkedHashMap<String, Object> {

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
}
