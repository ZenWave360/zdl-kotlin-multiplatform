package io.github.zenwave360.zdl.antlr;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import java.util.List;
import java.util.Set;

class JSONPath {

    private static final Configuration config = Configuration.defaultConfiguration();

    public static <T> T get(Object object, String jsonPath) {
        try {
            return (T) JsonPath.using(config).parse(object).read(jsonPath);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    public static <T> List<T> getUnique(Object object, String jsonPath) {
        return get(object, jsonPath, List.<T>of()).stream().filter(e -> e != null).distinct().toList();
    }

    public static <T> T get(Object object, String jsonPath, T defaultIfNull) {
        try {
            var value = JsonPath.using(config).parse(object).read(jsonPath);
            return value != null? (T) value : defaultIfNull;
        } catch (PathNotFoundException e) {
            return defaultIfNull;
        }
    }
}
