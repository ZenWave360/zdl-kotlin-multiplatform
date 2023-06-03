package io.github.zenwave360.zdl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ZdlModel extends FluentMap {
    public ZdlModel() {
        put("config", new FluentMap());
        put("apis", new FluentMap());
        put("entities", new FluentMap());
        put("enums", new FluentMap());
        put("relationships", new FluentMap());
        put("services", new FluentMap());
        put("inputs", new FluentMap());
        put("outputs", new FluentMap());
        put("events", new FluentMap());
        put("locations", new FluentMap());
    }

    public FluentMap getEntities() {
        return (FluentMap) get("entities");
    }

    public FluentMap getInputs() {
        return (FluentMap) get("inputs");
    }

    public FluentMap getOutputs() {
        return (FluentMap) get("outputs");
    }

    public FluentMap getEnums() {
        var enumsRoot = (FluentMap) get("enums");
        if(enumsRoot != null) {
            return (FluentMap) enumsRoot.get("enums");
        }
        return null;
    }

    public FluentMap getRelationships() {
        return (FluentMap) get("relationships");
    }

    public FluentMap getLocations() {
        return (FluentMap) get("locations");
    }

    public FluentMap setLocation(String location, int[] locations) {
        if(locations == null || locations.length != 4) {
            return this;
        }
        return appendTo("locations", location, locations);
    }

}
