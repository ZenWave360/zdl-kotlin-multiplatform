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

        var fields = JSONPath.get(model, "$..fields[*]", List.<Map<String, Object>>of());
        for (var field : fields) {
            if(entities != null && entities.containsKey(field.get("name"))) {
                field.put("isEntity", true);
            }
            if(enums != null && enums.containsKey(field.get("name"))) {
                field.put("isEnum", true);
            }
            if(inputs != null && inputs.containsKey(field.get("name"))) {
                field.put("isInput", true);
            }
            if(outputs != null && outputs.containsKey(field.get("name"))) {
                field.put("isOutput", true);
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
