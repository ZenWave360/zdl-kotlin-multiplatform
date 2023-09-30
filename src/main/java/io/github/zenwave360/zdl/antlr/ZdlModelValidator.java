package io.github.zenwave360.zdl.antlr;

import io.github.zenwave360.zdl.ZdlParser;

import java.util.List;
import java.util.Map;

public class ZdlModelValidator {
    public static ZdlModel validate(ZdlModel model) {
        validateEntitiesFields(model, "entities");
        validateEntitiesFields(model, "inputs");
        validateEntitiesFields(model, "outputs");
        validateEntitiesFields(model, "events");
        validateServices(model);
        validateRelationships(model);
        return model;
    }

    private static void validateRelationships(ZdlModel model) {
        var relationships = JSONPath.get(model, "$.relationships[*][*]", List.<Map<String, Map>>of());
        for (Map relationship : relationships) {
            var type = (String) JSONPath.get(relationship, "$.type");
            var name = (String) JSONPath.get(relationship, "$.name");
            var from = (String) JSONPath.get(relationship, "$.from");
            var to = (String) JSONPath.get(relationship, "$.to");
            var injectedFieldInFrom = (String) JSONPath.get(relationship, "$.injectedFieldInFrom");
            var injectedFieldInTo = (String) JSONPath.get(relationship, "$.injectedFieldInTo");
            if(!isEntity(model, from)) {
                model.addProblem(path("relationships", name, "from", "entity"), from, "%s is not a valid entity");
            }
            if(!isEntity(model, to)) {
                model.addProblem(path("relationships", name, "to", "entity"), to, "%s is not a valid entity");
            }
            // TODO: validate injectedFieldInFrom and injectedFieldInTo
        }
    }

    private static void validateEntitiesFields(ZdlModel model, String type) {
        var entities = JSONPath.get(model, "$." + type, Map.<String, Object>of());
        for (Map.Entry<String, Object> entity : entities.entrySet()) {
            validateFields(model, (Map) entity.getValue());
        }
    }

    private static void validateFields(ZdlModel model, Map entity) {
        String entityType = (String) JSONPath.get(entity, "$.type");
        String entityName = (String) JSONPath.get(entity, "$.name");
        var fields = JSONPath.get(entity, "$.fields", Map.<String, Map>of());
        for (Map field : fields.values()) {
            var fieldName = (String) JSONPath.get(field, "$.name");
            var fieldType = (String) JSONPath.get(field, "$.type");
            validateField(model, entityType, entityName, fieldName, fieldType);
        }
    }

    private static void validateField(ZdlModel model, String entityType, String entityName, String fieldName, String fieldType) {
        if("entity".equals(entityType)) {
            if(!(isEntityOrEnum(model, fieldType) || isStandardFieldType(fieldType))) {
                model.addProblem(path("entities", entityName, "fields", fieldName, "type"), fieldType, "%s is not a valid type");
            }
        }
        else if ("input".equals(entityType)) {
            if(!(isEntityOrEnum(model, fieldType) || isStandardFieldType(fieldType) || isInput(model, fieldType))) {
                model.addProblem(path("inputs", entityName, "fields", fieldName, "type"), fieldType, "%s is not a valid type");
            }
        }
        else if ("output".equals(entityType)) {
            if(!(isEntityOrEnum(model, fieldType) || isStandardFieldType(fieldType) || isInput(model, fieldType) || isOutput(model, fieldType))) {
                model.addProblem(path("outputs", entityName, "fields", fieldName, "type"), fieldType, "%s is not a valid type");
            }
        }
        else if ("event".equals(entityType)) {
            if (!(isEntityOrEnum(model, fieldType) || isStandardFieldType(fieldType) || isEvent(model, fieldType))) {
                model.addProblem(path("events", entityName, "fields", fieldName, "type"), fieldType, "%s is not a valid type");
            }
        }
    }

    private static List<Map> validateServices(ZdlModel model) {
        var services = JSONPath.get(model, "$.services", Map.<String, Object>of());
        for (Map.Entry<String, Object> service : services.entrySet()) {
            var aggregates = JSONPath.get(service.getValue(), "$.aggregates", List.<String>of());
            for (String aggregate : aggregates) {
                if(!aggregate.isEmpty() && !isAggregate(model, aggregate)) {
                    model.addProblem(path("services", service.getKey(), "aggregates"),  aggregate,"%s is not an aggregate");
                }
            }

            var methods = JSONPath.get(service.getValue(), "$.methods[*]", List.<Map>of());
            for (Map method : methods) {
                var methodName = (String) JSONPath.get(method, "$.name");
                var parameter = (String) JSONPath.get(method, "$.parameter");
                if(parameter != null && !isEntity(model, parameter) && !isInput(model, parameter)) {
                    model.addProblem(path("services", service.getKey(), "methods", methodName, "parameter"), parameter, "%s is not an entity or input");
                }
                var returnType = (String) JSONPath.get(method, "$.returnType");
                if(returnType != null && !isEntity(model, returnType) && !isOutput(model, returnType)) {
                    model.addProblem(path("services", service.getKey(), "methods", methodName, "returnType"), returnType, "%s is not an entity or output");
                }
            }
        }

        return null;
    }

    private static String path(String... path) {
        return String.join(".", path);
    }

    private static boolean isStandardFieldType(String fieldType) {
        return ZdlParser.STANDARD_FIELD_TYPES.contains(fieldType);
    }

    private static boolean isEntity(ZdlModel model, String entityName) {
        return JSONPath.get(model, "$.entities." + entityName) != null;
    }

    private static boolean isEnum(ZdlModel model, String entityName) {
        return JSONPath.get(model, "$.enums.enums." + entityName) != null;
    }

    private static boolean isInput(ZdlModel model, String entityName) {
        return JSONPath.get(model, "$.inputs." + entityName) != null;
    }

    private static boolean isOutput(ZdlModel model, String entityName) {
        return JSONPath.get(model, "$.outputs." + entityName) != null;
    }

    private static boolean isEvent(ZdlModel model, String entityName) {
        return JSONPath.get(model, "$.events." + entityName) != null;
    }

    private static boolean isEntityOrEnum(ZdlModel model, String entityName) {
        return isEntity(model, entityName) || isEnum(model, entityName);
    }

    private static boolean isAggregate(ZdlModel model, String entityName) {
        return JSONPath.get(model, "$.entities." + entityName + ".options.aggregate", false);
    }
}
