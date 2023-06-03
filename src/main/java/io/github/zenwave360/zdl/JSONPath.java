package io.github.zenwave360.zdl;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

class JSONPath {

    private static final Configuration config = Configuration.defaultConfiguration();

    public static <T> T get(Object object, String jsonPath) {
        try {
            return (T) JsonPath.using(config).parse(object).read(jsonPath);
        } catch (PathNotFoundException e) {
            return null;
        }
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
