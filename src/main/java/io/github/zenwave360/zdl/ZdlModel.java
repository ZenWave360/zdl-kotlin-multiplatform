package io.github.zenwave360.zdl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ZdlModel extends FluentMap {
    public ZdlModel() {
        put("options", new FluentMap());
        put("entities", new FluentMap());
        put("enums", new FluentMap());
        put("inputs", new FluentMap());
        put("services", new FluentMap());
        put("events", new FluentMap());
    }

    public FluentMap getEntities() {
        return (FluentMap) get("entities");
    }

}
