package io.github.zenwave360.zdl.antlr;

import io.github.zenwave360.zdl.ZdlParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.github.zenwave360.zdl.ZdlParser.STANDARD_FIELD_TYPES;

public class ZdlModelValidator {

    private List<String> API_ROLES = List.of("provider", "client");
    
    private List<String> standardFieldTypes = STANDARD_FIELD_TYPES;
    private List<String> extraFieldTypes = List.of();

    public ZdlModelValidator withStandardFieldTypes(List<String> standardFieldTypes) {
        this.standardFieldTypes = standardFieldTypes;
        return this;
    }

    public ZdlModelValidator withExtraFieldTypes(List<String> extraFieldTypes) {
        this.extraFieldTypes = extraFieldTypes;
        return this;
    }

    public ZdlModel validate(ZdlModel model) {
        model.clearProblems();
        validateApis(model);
        validateEntitiesFields(model, "entities");
        validateEntitiesFields(model, "inputs");
        validateEntitiesFields(model, "outputs");
        validateEntitiesFields(model, "events");
        validateServices(model);
        validateRelationships(model);
        return model;
    }

    private void validateApis(ZdlModel model) {
        var apis = JSONPath.get(model, "$.apis[*]", List.<Map<String, Map>>of());
        for (Map api : apis) {
            if(!API_ROLES.contains(api.get("role"))) {
                model.addProblem(path("apis", (String) api.get("name"), "role"), (String) api.get("role"), "%s is not a valid API role [provider|client]");
            }
        }
    }

    private void validateRelationships(ZdlModel model) {
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

    private void validateEntitiesFields(ZdlModel model, String type) {
        var entities = JSONPath.get(model, "$." + type, Map.<String, Object>of());
        for (Map.Entry<String, Object> entity : entities.entrySet()) {
            validateFields(model, (Map) entity.getValue());
        }
    }

    private void validateFields(ZdlModel model, Map entity) {
        String entityType = (String) JSONPath.get(entity, "$.type");
        String entityName = (String) JSONPath.get(entity, "$.name");
        var fields = JSONPath.get(entity, "$.fields", Map.<String, Map>of());
        for (Map field : fields.values()) {
            var fieldName = (String) JSONPath.get(field, "$.name");
            var fieldType = (String) JSONPath.get(field, "$.type");
            validateField(model, entityType, entityName, fieldName, fieldType);
        }
    }

    private void validateField(ZdlModel model, String entityType, String entityName, String fieldName, String fieldType) {
        if("entities".equals(entityType)) {
            if(!(isStandardFieldType(fieldType) || isEntityOrEnum(model, fieldType))) {
                model.addProblem(path("entities", entityName, "fields", fieldName, "type"), fieldType, "%s is not a valid type");
            }
        }
        else if ("inputs".equals(entityType)) {
            if(!(isStandardFieldType(fieldType) || isEntityOrEnum(model, fieldType) || isInput(model, fieldType))) {
                model.addProblem(path("inputs", entityName, "fields", fieldName, "type"), fieldType, "%s is not a valid type");
            }
        }
        else if ("outputs".equals(entityType)) {
            if(!(isStandardFieldType(fieldType) || isEntityOrEnum(model, fieldType) || isInput(model, fieldType) || isOutput(model, fieldType))) {
                model.addProblem(path("outputs", entityName, "fields", fieldName, "type"), fieldType, "%s is not a valid type");
            }
        }
        else if ("events".equals(entityType)) {
            if (!(isStandardFieldType(fieldType) || isEntityOrEnum(model, fieldType) || isEvent(model, fieldType))) {
                model.addProblem(path("events", entityName, "fields", fieldName, "type"), fieldType, "%s is not a valid type");
            }
        }
    }

    private List<Map> validateServices(ZdlModel model) {
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
                List<Object> withEvents = (List) method.getOrDefault("withEvents", List.of());
                for (int i = 0; i < withEvents.size(); i++) {
                    var event = withEvents.get(i);
                    if (event instanceof List) {
                        for (int j = 0; j < ((List<?>) event).size(); j++) {
                            var innerEvent = (String) ((List<?>) event).get(j);
                            if(!isEvent(model, innerEvent)) {
                                model.addProblem(path("services", service.getKey(), "methods", methodName, "withEvents", i+"", j+""), innerEvent, "%s is not an event");
                            }
                        }
                    } else {
                        if(!isEvent(model, (String) event)) {
                            model.addProblem(path("services", service.getKey(), "methods", methodName, "withEvents", i+""), (String) event, "%s is not an event");
                        }
                    }
                }
            }
        }

        return null;
    }

    private String path(String... path) {
        return String.join(".", path);
    }

    private boolean isStandardFieldType(String fieldType) {
        return fieldType != null && (standardFieldTypes.contains(fieldType) || extraFieldTypes.contains(fieldType));
    }

    private boolean isEntity(ZdlModel model, String entityName) {
        return JSONPath.get(model, "$.entities." + entityName) != null;
    }

    private boolean isEnum(ZdlModel model, String entityName) {
        return JSONPath.get(model, "$.enums." + entityName) != null;
    }

    private boolean isInput(ZdlModel model, String entityName) {
        return JSONPath.get(model, "$.inputs." + entityName) != null;
    }

    private boolean isOutput(ZdlModel model, String entityName) {
        return JSONPath.get(model, "$.outputs." + entityName) != null;
    }

    private boolean isEvent(ZdlModel model, String entityName) {
        return JSONPath.get(model, "$.events." + entityName) != null;
    }

    private boolean isEntityOrEnum(ZdlModel model, String entityName) {
        return isEntity(model, entityName) || isEnum(model, entityName);
    }

    private boolean isAggregate(ZdlModel model, String entityName) {
        return JSONPath.get(model, "$.entities." + entityName + ".options.aggregate", false);
    }

    private List<String> methodEventsFlatList(Map<String, Object> method) {
        var events = (List) method.getOrDefault("withEvents", List.of());
        List<String> allEvents = new ArrayList<>();
        for (Object event : events) {
            if(event instanceof String) {
                allEvents.add((String) event);
            } else if(event instanceof List) {
                allEvents.addAll((Collection<? extends String>) event);
            }
        }
        return allEvents;
    }
}
