package io.github.zenwave360.zdl.antlr;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.camelCase;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.createCRUDMethods;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.first;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.getArray;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.getComplexValue;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.getLocations;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.getObject;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.getOptionValue;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.getText;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.getValueText;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.javadoc;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.kebabCase;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.lowerCamelCase;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.pluralize;
import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.snakeCase;

public class ZdlListenerImpl extends io.github.zenwave360.zdl.antlr.ZdlBaseListener {

    ZdlModel model = new ZdlModel();
    Stack<FluentMap> currentStack = new Stack<>();
    String currentCollection = null;

    public ZdlModel getModel() {
        return model;
    }

    @Override
    public void enterZdl(io.github.zenwave360.zdl.antlr.ZdlParser.ZdlContext ctx) {

    }

    @Override
    public void enterSuffix_javadoc(io.github.zenwave360.zdl.antlr.ZdlParser.Suffix_javadocContext ctx) {
        super.enterSuffix_javadoc(ctx);
    }

    @Override
    public void enterGlobal_javadoc(io.github.zenwave360.zdl.antlr.ZdlParser.Global_javadocContext ctx) {
        var javadoc = getText(ctx);
        model.put("javadoc", javadoc(javadoc));
    }

    @Override
    public void enterLegacy_constants(io.github.zenwave360.zdl.antlr.ZdlParser.Legacy_constantsContext ctx) {
        ctx.LEGACY_CONSTANT().stream().map(TerminalNode::getText).map(c -> c.split(" *= *")).forEach(c -> model.appendTo("constants", c[0], c[1]));
    }

    @Override
    public void enterConfig_option(io.github.zenwave360.zdl.antlr.ZdlParser.Config_optionContext ctx) {
        var name = ctx.field_name().getText();
        var value = getComplexValue(ctx.complex_value());
        model.appendTo("config", name, value);
    }

    @Override
    public void enterApi(io.github.zenwave360.zdl.antlr.ZdlParser.ApiContext ctx) {
        var name = ctx.api_name().getText();
        var type = ctx.api_type().getText();
        var role = ctx.api_role().getText();
        var javadoc = getText(ctx.javadoc());
        currentStack.push(new FluentMap()
                .with("name", name)
                .with("type", type)
                .with("role", role)
                .with("javadoc", javadoc)
                .with("options", new FluentMap())
                .with("config", new FluentMap())
        );
        model.appendTo("apis", name, currentStack.peek());
    }

    @Override
    public void enterApi_config(io.github.zenwave360.zdl.antlr.ZdlParser.Api_configContext ctx) {
        var name = ctx.field_name().getText();
        var value = getComplexValue(ctx.complex_value());
        currentStack.peek().appendTo("config", name, value);
    }

    @Override
    public void exitApi(io.github.zenwave360.zdl.antlr.ZdlParser.ApiContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterEntity(io.github.zenwave360.zdl.antlr.ZdlParser.EntityContext ctx) {
        var entity = ctx.entity_definition();
        var name = entity.entity_name().getText();
        var javadoc = getText(ctx.javadoc());
        var tableName = entity.entity_table_name() != null? entity.entity_table_name().ID().getText() : null;
        currentStack.push(processEntity(name, javadoc, tableName).with("type", "entity"));
        model.appendTo("entities", name, currentStack.peek());
        currentCollection = "entities";

        var entityLocation = currentCollection + "." + name;
        model.setLocation(entityLocation, getLocations(ctx));
        model.setLocation(entityLocation + ".name", getLocations(entity.entity_name()));
        model.setLocation(entityLocation + ".tableName", getLocations(entity.entity_table_name()));
        model.setLocation(entityLocation + ".body", getLocations(ctx.entity_body()));
    }

    @Override
    public void exitEntity(io.github.zenwave360.zdl.antlr.ZdlParser.EntityContext ctx) {
        currentStack.pop();
    }

    private FluentMap processEntity(String name, String javadoc, String tableName) {
        var className = camelCase(name);
        var instanceName = lowerCamelCase(className);
        var kebabCase = kebabCase(name);
        return new FluentMap()
                .with("name", name)
                .with("className", className)
                .with("tableName", tableName != null? tableName : snakeCase(name))
                .with("instanceName", instanceName)
                .with("classNamePlural", pluralize(name))
                .with("instanceNamePlural", pluralize(instanceName))
                .with("kebabCase", kebabCase)
                .with("kebabCasePlural", pluralize(kebabCase))
                .with("javadoc", javadoc(javadoc))
                .with("options", new FluentMap())
                .with("fields", new FluentMap())
        ;
    }

    @Override
    public void enterOption(io.github.zenwave360.zdl.antlr.ZdlParser.OptionContext ctx) {
//        var name = ctx.reserved_option() != null? ctx.reserved_option().getText().replace("@", "") : getText(ctx.option_name());
        var name = ctx.option_name().getText().replace("@", "");
        var value = getOptionValue(ctx.option_value());
        if(!currentStack.isEmpty()) {
            currentStack.peek().appendTo("options", name, value);
            currentStack.peek().appendToList("optionsList", new FluentMap().with("name", name).with("value", value));
        }
        super.enterOption(ctx);
    }

    @Override
    public void enterField(io.github.zenwave360.zdl.antlr.ZdlParser.FieldContext ctx) {
        var name = getText(ctx.field_name());
        var type = ctx.field_type() != null && ctx.field_type().ID() != null? ctx.field_type().ID().getText() : null;
        var javadoc = first(getText(ctx.javadoc(), getText(ctx.suffix_javadoc())));
        var isEnum = false; // TODO
        var isEntity = false; // TODO
        var isArray = ctx.field_type().ARRAY() != null;
        var validations = processFieldValidations(ctx.field_validations());
        currentStack.peek().appendTo("fields", name, new FluentMap()
                .with("name", name)
                .with("type", type)
                .with("javadoc", javadoc(javadoc))
                .with("comment", javadoc(javadoc))
                .with("isEnum", isEnum)
                .with("isEntity", isEntity)
                .with("isArray", isArray)
                .with("options", new FluentMap())
                .with("validations", validations));

        var entityName =  currentStack.peek().get("name");
        var entityLocation = currentCollection + "." + entityName + ".fields." + name;
        model.setLocation(entityLocation, getLocations(ctx));
        model.setLocation(entityLocation + ".name", getLocations(ctx.field_name()));
        model.setLocation(entityLocation + ".type", getLocations(ctx.field_type()));
        model.setLocation(entityLocation + ".javadoc", getLocations(first(ctx.javadoc(), ctx.suffix_javadoc())));
    }

    private Map<String, Object> processFieldValidations(List<io.github.zenwave360.zdl.antlr.ZdlParser.Field_validationsContext> field_validations) {
        var validations = new FluentMap();
        if(field_validations != null) {
            field_validations.forEach(v -> {
                var name = getText(v.field_validation_name());
                var value = first(getText(v.field_validation_value()), "");
                validations.with(name, Map.of("name", name, "value", value));
            });
        }
        return validations;
    }

    @Override
    public void exitField(io.github.zenwave360.zdl.antlr.ZdlParser.FieldContext ctx) {
        super.exitField(ctx);
    }

    @Override
    public void enterNested_field(io.github.zenwave360.zdl.antlr.ZdlParser.Nested_fieldContext ctx) {
        io.github.zenwave360.zdl.antlr.ZdlParser.FieldContext parent = (io.github.zenwave360.zdl.antlr.ZdlParser.FieldContext) ctx.getParent();
        var parentEntity = currentStack.peek();
        var parentEntityFields = ((FluentMap) parentEntity.get("fields"));
        var parentField = new ArrayList<>(parentEntityFields.values()).get(parentEntityFields.size() - 1);
        String entityName = parent.field_type().ID().getText();
        String entityJavadoc = getText(parent.javadoc());
        String tableName = parent.entity_table_name() != null? parent.entity_table_name().ID().getText() : null;
        var validations = processNestedFieldValidations(ctx.nested_field_validations());
        ((Map)parentField).put("validations", validations);
        currentStack.push(processEntity(entityName, entityJavadoc, tableName).with("type", currentCollection.split("\\.")[0]));
        currentStack.peek().appendTo("options", "embedded", true);
        model.appendTo(currentCollection, entityName, currentStack.peek());
    }

    private Map<String, Object> processNestedFieldValidations(List<io.github.zenwave360.zdl.antlr.ZdlParser.Nested_field_validationsContext> field_validations) {
        var validations = new FluentMap();
        if(field_validations != null) {
            field_validations.forEach(v -> {
                var name = getText(v.nested_field_validation_name());
                var value = first(getText(v.nested_field_validation_value()), "");
                validations.with(name, Map.of("name", name, "value", value));
            });
        }
        return validations;
    }

    @Override
    public void exitNested_field(io.github.zenwave360.zdl.antlr.ZdlParser.Nested_fieldContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterEnum(io.github.zenwave360.zdl.antlr.ZdlParser.EnumContext ctx) {
        var name = getText(ctx.enum_name());
        var javadoc = getText(ctx.javadoc());
        currentStack.push(new FluentMap()
                .with("name", name)
                .with("type", "enums")
                .with("className", camelCase(name))
                .with("javadoc", javadoc(javadoc))
                .with("comment", javadoc(javadoc)));
        ((FluentMap) model.get("enums")).appendTo("enums", name, currentStack.peek());

        var entityLocation = "enums.enums." + name;
        model.setLocation(entityLocation, getLocations(ctx));
        model.setLocation(entityLocation + ".name", getLocations(ctx.enum_name()));
        model.setLocation(entityLocation + ".body", getLocations(ctx.enum_body()));
    }

    @Override
    public void exitEnum(io.github.zenwave360.zdl.antlr.ZdlParser.EnumContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterEnum_value(io.github.zenwave360.zdl.antlr.ZdlParser.Enum_valueContext ctx) {
        var name = getText(ctx.enum_value_name());
        var javadoc = first(getText(ctx.javadoc(), getText(ctx.suffix_javadoc())));
        var value = ctx.enum_value_value() != null? getValueText(ctx.enum_value_value().simple()) : null;
        currentStack.peek().appendTo("values", name, new FluentMap()
                .with("name", name)
                .with("javadoc", javadoc(javadoc))
                .with("comment", javadoc(javadoc))
                .with("value", value)
        );
    }

    @Override
    public void enterRelationship(io.github.zenwave360.zdl.antlr.ZdlParser.RelationshipContext ctx) {
        var parent = (io.github.zenwave360.zdl.antlr.ZdlParser.RelationshipsContext) ctx.parent;
        var relationshipType = parent.relationship_type().getText();

        var from = getText(ctx.relationship_from().relationship_definition().relationship_entity_name());
        var fromField = getText(ctx.relationship_from().relationship_definition().relationship_field_name());
        var commentInFrom = getText(ctx.relationship_from().javadoc());
        var fromOptions = relationshipOptions(ctx.relationship_from().annotations().option());

        var to = getText(ctx.relationship_to().relationship_definition().relationship_entity_name());
        var toField = getText(ctx.relationship_to().relationship_definition().relationship_field_name());
        var commentInTo = getText(ctx.relationship_to().javadoc());
        var toOptions = relationshipOptions(ctx.relationship_to().annotations().option());

        var relationship = new FluentMap()
                .with("type", relationshipType)
                .with("from", from)
                .with("to", to)
                .with("commentInFrom", javadoc(commentInFrom))
                .with("commentInTo", javadoc(commentInTo))
                .appendTo("options", "source", fromOptions)
                .appendTo("options", "destination", toOptions)
                .with("injectedFieldInFrom", fromField) // FIXME review this
                .with("injectedFieldInTo", toField) // FIXME review this
                .with("isInjectedFieldInFromRequired", false) // FIXME review this
                .with("isInjectedFieldInToRequired", false) // FIXME review this
                ;
        var relationshipName = relationshipType + "_" + ctx.relationship_from().getText() + "_" + ctx.relationship_to().getText();
        model.getRelationships().appendTo(relationshipType, relationshipName, relationship);
    }

    private Map<String, Object> relationshipOptions(List<io.github.zenwave360.zdl.antlr.ZdlParser.OptionContext> options) {
        return options.stream().collect(Collectors.toMap(o -> getText(o.option_name()), o -> getOptionValue(o.option_value())));
    }

    @Override
    public void enterService_legacy(io.github.zenwave360.zdl.antlr.ZdlParser.Service_legacyContext ctx) {
        var serviceName = ctx.ID().getText();
        String serviceJavadoc = null; // getText(ctx.javadoc());
        var serviceAggregates = ctx.service_aggregates() != null? Arrays.asList(ctx.service_aggregates().getText().split(",")) : null;
        currentStack.push(new FluentMap()
                .with("name", serviceName)
                .with("isLegacy", true)
                .with("className", camelCase(serviceName))
                .with("javadoc", javadoc(serviceJavadoc))
                .with("aggregates", serviceAggregates)
                .with("methods", createCRUDMethods(serviceAggregates))
        );
        model.appendTo("services", serviceName, currentStack.peek());
    }

    @Override
    public void exitService_legacy(io.github.zenwave360.zdl.antlr.ZdlParser.Service_legacyContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterService(io.github.zenwave360.zdl.antlr.ZdlParser.ServiceContext ctx) {
        var serviceName = ctx.ID().getText();
        var serviceJavadoc = getText(ctx.javadoc());
        var serviceAggregates = ctx.service_aggregates() != null? Arrays.asList(ctx.service_aggregates().getText().split(",")) : null;
        currentStack.push(new FluentMap()
                .with("name", serviceName)
                .with("className", camelCase(serviceName))
                .with("javadoc", javadoc(serviceJavadoc))
                .with("aggregates", serviceAggregates)
                .with("methods", new FluentMap())
        );
        model.appendTo("services", serviceName, currentStack.peek());
    }

    @Override
    public void exitService(io.github.zenwave360.zdl.antlr.ZdlParser.ServiceContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterService_method(io.github.zenwave360.zdl.antlr.ZdlParser.Service_methodContext ctx) {
        var methodName = getText(ctx.service_method_name());
        var methodParamId = ctx.service_method_parameter_id() != null? "id" : null;
        var methodParameter = ctx.service_method_parameter() != null? ctx.service_method_parameter().getText() : null;
        var returnType = ctx.service_method_return() != null? ctx.service_method_return().ID().getText() : null;
        var returnTypeIsArray = ctx.service_method_return() != null? ctx.service_method_return().ARRAY() != null : null;
        var returnTypeIsOptional = ctx.service_method_return() != null? ctx.service_method_return().OPTIONAL() != null : null;
        var withEvents = getServiceMethodEvents(ctx.service_method_with_events());
        var javadoc = first(getText(ctx.javadoc(), getText(ctx.suffix_javadoc())));

        var method = new FluentMap()
                .with("name", methodName)
                .with("paramId", methodParamId)
                .with("parameter", methodParameter)
                .with("returnType", returnType)
                .with("returnTypeIsArray", returnTypeIsArray)
                .with("returnTypeIsOptional", returnTypeIsOptional)
                .with("withEvents", withEvents)
                .with("javadoc", javadoc)
                ;
        currentStack.peek().appendTo("methods", methodName, method);
        currentStack.push(method);
    }

    @Override
    public void exitService_method(io.github.zenwave360.zdl.antlr.ZdlParser.Service_methodContext ctx) {
        currentStack.pop();
    }

    private List<Object> getServiceMethodEvents(io.github.zenwave360.zdl.antlr.ZdlParser.Service_method_with_eventsContext ctx) {
        var events = new ArrayList<>();
        if (ctx != null) {
            ctx.service_method_events().forEach(event -> {
                if (event.service_method_event() != null) {
                    events.add(getText(event.service_method_event()));
                }
                if (event.service_method_events_or() != null) {
                    var orEvents = event.service_method_events_or().ID().stream().map(ParseTree::getText).collect(Collectors.toList());
                    events.add(orEvents);
                }
            });
        }
        return events;
    }

    @Override
    public void enterEvent(io.github.zenwave360.zdl.antlr.ZdlParser.EventContext ctx) {
        var name = ctx.event_name().getText();
        var channel = getText(ctx.event_channel());
        var javadoc = getText(ctx.javadoc());
        var kebabCase = kebabCase(name);
        currentStack.push(new FluentMap()
                .with("name", name)
                .with("type", "events")
                .with("channel", channel)
                .with("kebabCase", kebabCase)
                .with("javadoc", javadoc(javadoc))
                .with("fields", new FluentMap())
        );
        model.appendTo("events", name, currentStack.peek());
        currentCollection = "events";
    }

    @Override
    public void exitEvent(io.github.zenwave360.zdl.antlr.ZdlParser.EventContext ctx) {
        currentStack.pop();
    }

    //    @Override
//    public void exitEvent(ZdlParser.EventContext ctx) {
//        ctx.option().stream().filter(o -> o.option_name().getText().equals("entity")).findFirst().ifPresent(o -> {
//            var entityName = o.option_value().getText();
//            var entity = model.getEntities().get(entityName);
//            var fields = ((Map)entity).get("fields");
//            current.appendTo("fields", (Map) fields);
//        });
//    }

    @Override
    public void enterInput(io.github.zenwave360.zdl.antlr.ZdlParser.InputContext ctx) {
        var name = ctx.input_name().getText();
        var javadoc = getText(ctx.javadoc());
        currentStack.push(processEntity(name, javadoc, null).with("type", "inputs"));
        model.appendTo("inputs", name, currentStack.peek());
        currentCollection = "inputs";
    }

    @Override
    public void exitInput(io.github.zenwave360.zdl.antlr.ZdlParser.InputContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterOutput(io.github.zenwave360.zdl.antlr.ZdlParser.OutputContext ctx) {
        var name = ctx.output_name().getText();
        var javadoc = getText(ctx.javadoc());
        currentStack.push(processEntity(name, javadoc, null).with("type", "outputs"));
        model.appendTo("outputs", name, currentStack.peek());
        currentCollection = "outputs";
    }

    @Override
    public void exitOutput(io.github.zenwave360.zdl.antlr.ZdlParser.OutputContext ctx) {
        currentStack.pop();
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        super.exitEveryRule(ctx);
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        super.visitTerminal(node);
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        super.visitErrorNode(node);
    }

}
