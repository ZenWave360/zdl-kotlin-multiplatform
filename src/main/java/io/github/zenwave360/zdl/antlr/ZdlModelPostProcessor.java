package io.github.zenwave360.zdl.antlr;

import io.github.zenwave360.zdl.antlr.JSONPath;
import io.github.zenwave360.zdl.antlr.ZdlModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZdlModelPostProcessor {

    public static ZdlModel postProcess(ZdlModel model) {
        var entities = model.getEntities();
        var inputs = model.getInputs();
        var outputs = model.getOutputs();
        var enums = model.getEnums();
        var events = model.getEvents();

        var fields = JSONPath.get(model, "$..fields[*]", List.<Map<String, Object>>of());
        for (var field : fields) {
            if(entities != null && entities.containsKey(field.get("type"))) {
                field.put("isEntity", true);
                field.put("isComplexType", true);
            }
            if(enums != null && enums.containsKey(field.get("type"))) {
                field.put("isEnum", true);
                field.put("isComplexType", true);
            }
            if(inputs != null && inputs.containsKey(field.get("type"))) {
                field.put("isInput", true);
                field.put("isComplexType", true);
            }
            if(outputs != null && outputs.containsKey(field.get("type"))) {
                field.put("isOutput", true);
                field.put("isComplexType", true);
            }
            if(events != null && events.containsKey(field.get("type"))) {
                field.put("isEvent", true);
                field.put("isComplexType", true);
            }
        }

        var allEntitiesAndEnums = new HashMap<>();
        if(entities != null) {
            allEntitiesAndEnums.putAll(entities);
        }
        if(enums != null) {
            allEntitiesAndEnums.putAll(enums);
        }
        if(inputs != null) {
            allEntitiesAndEnums.putAll(inputs);
        }
        if(outputs != null) {
            allEntitiesAndEnums.putAll(outputs);
        }
        model.put("allEntitiesAndEnums", allEntitiesAndEnums);

        return model;
    }
}
