package io.github.zenwave360.zdl.antlr;

import io.github.zenwave360.zdl.FluentMap;
import io.github.zenwave360.zdl.ZdlModel;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import static io.github.zenwave360.zdl.antlr.ZdlListenerUtils.*;

public class ZdlListenerImpl extends ZdlBaseListener {

    ZdlModel model = new ZdlModel();
    Stack<FluentMap> currentStack = new Stack<>();
    String currentCollection = null;

    public ZdlModel getModel() {
        return model;
    }

    @Override
    public void enterZdl(ZdlParser.ZdlContext ctx) {

    }

    @Override
    public void enterGlobal_javadoc(ZdlParser.Global_javadocContext ctx) {
        var javadoc = getText(ctx);
        model.put("javadoc", javadoc(javadoc));
    }

    @Override
    public void enterLegacy_constants(ZdlParser.Legacy_constantsContext ctx) {
        ctx.LEGACY_CONSTANT().stream().map(TerminalNode::getText).map(c -> c.split("=")).forEach(c -> model.appendTo("constants", c[0], c[1]));
    }

    @Override
    public void enterConfig_option(ZdlParser.Config_optionContext ctx) {
        var name = ctx.option_name().getText();
        var value = ctx.option_value() != null? getValueText(ctx.option_value().value()) : null;
        var array = ctx.option_value() != null? getArray(ctx.option_value().array()) : null;
        var object = ctx.option_value() != null? getObject(ctx.option_value().object()) : null;
        model.appendTo("config", name, first(value, array, object, true));
    }

    @Override
    public void enterApi(ZdlParser.ApiContext ctx) {
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
    public void enterApi_config(ZdlParser.Api_configContext ctx) {
        var name = ctx.option_name().getText();
        var value = ctx.option_value() != null? getValueText(ctx.option_value().value()) : null;
        var array = ctx.option_value() != null? getArray(ctx.option_value().array()) : null;
        var object = ctx.option_value() != null? getObject(ctx.option_value().object()) : null;
        currentStack.peek().appendTo("config", name, first(value, array, object, true));
    }

    @Override
    public void exitApi(ZdlParser.ApiContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterEntity(ZdlParser.EntityContext ctx) {
        var name = ctx.entity_name().getText();
        var javadoc = getText(ctx.javadoc());
        var tableName = ctx.entity_table_name() != null? ctx.entity_table_name().ID().getText() : null;
        currentStack.push(processEntity(name, javadoc, tableName));
        model.appendTo("entities", name, currentStack.peek());
        currentCollection = "entities";

        var entityLocation = currentCollection + "." + name;
        model.setLocation(entityLocation, getLocations(ctx));
        model.setLocation(entityLocation + ".name", getLocations(ctx.entity_name()));
        model.setLocation(entityLocation + ".tableName", getLocations(ctx.entity_table_name()));
        model.setLocation(entityLocation + ".body", getLocations(ctx.entity_body()));
    }

    @Override
    public void exitEntity(ZdlParser.EntityContext ctx) {
        currentStack.pop();
    }

    private FluentMap processEntity(String name, String javadoc, String tableName) {
        var className = camelCase(name);
        var instanceName = lowerCamelCase(className);
        var kebabCase = kebabCase(name);
        return new FluentMap()
                .with("name", name)
                .with("className", className)
                .with("instanceName", instanceName)
                .with("classNamePlural", pluralize(name))
                .with("instanceNamePlural", pluralize(instanceName))
                .with("kebabCase", kebabCase)
                .with("kebabCasePlural", pluralize(kebabCase))
                .with("tableName", tableName)
                .with("javadoc", javadoc(javadoc))
                .with("options", new FluentMap())
                .with("fields", new FluentMap())
        ;
    }

    @Override
    public void enterOption(ZdlParser.OptionContext ctx) {
        var name = ctx.reserved_option() != null? ctx.reserved_option().getText().replace("@", "") : getText(ctx.option_name());
        var value = ctx.option_value() != null? getValueText(ctx.option_value().value()) : null;
        var array = ctx.option_value() != null? getArray(ctx.option_value().array()) : null;
        var object = ctx.option_value() != null? getObject(ctx.option_value().object()) : null;
        currentStack.peek().appendTo("options", name, first(value, array, object, true));
        super.enterOption(ctx);
    }

    @Override
    public void enterField(ZdlParser.FieldContext ctx) {
        var name = getText(ctx.field_name());
        var type = ctx.field_type().ID().getText();
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

    private Map<String, Object> processFieldValidations(List<ZdlParser.Field_validationsContext> field_validations) {
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
    public void exitField(ZdlParser.FieldContext ctx) {
        super.exitField(ctx);
    }

    @Override
    public void enterNested_field(ZdlParser.Nested_fieldContext ctx) {
        ZdlParser.FieldContext parent = (ZdlParser.FieldContext) ctx.getParent();
        String entityName = parent.field_type().ID().getText();
        String entityJavadoc = getText(parent.javadoc());
        String tableName = parent.entity_table_name() != null? parent.entity_table_name().ID().getText() : null;
        currentStack.push(processEntity(entityName, entityJavadoc, tableName));
        currentStack.peek().appendTo("options", "embedded", true);
        model.appendTo(currentCollection, entityName, currentStack.peek());
    }

    @Override
    public void exitNested_field(ZdlParser.Nested_fieldContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterEnum(ZdlParser.EnumContext ctx) {
        var name = getText(ctx.enum_name());
        var javadoc = getText(ctx.javadoc());
        currentStack.push(new FluentMap()
                .with("name", name)
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
    public void exitEnum(ZdlParser.EnumContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterEnum_value(ZdlParser.Enum_valueContext ctx) {
        var name = getText(ctx.enum_value_name());
        var javadoc = first(getText(ctx.javadoc(), getText(ctx.suffix_javadoc())));
        var value = ctx.enum_value_value() != null? getValueText(ctx.enum_value_value().value()) : null;
        currentStack.peek().appendTo("values", name, new FluentMap()
                .with("name", name)
                .with("javadoc", javadoc(javadoc))
                .with("comment", javadoc(javadoc))
                .with("value", value)
        );
    }

    @Override
    public void enterRelationship(ZdlParser.RelationshipContext ctx) {
        var parent = (ZdlParser.RelationshipsContext) ctx.parent;
        var relationshipType = parent.relationship_type().getText();

        var from = getText(ctx.relationship_from().relationship_definition().relationship_entity_name());
        var fromField = getText(ctx.relationship_from().relationship_definition().relationship_field_name());
        var commentInFrom = getText(ctx.relationship_from().relationship_javadoc());
        var fromOptions = relationshipOptions(ctx.relationship_from().relationship_options().option());

        var to = getText(ctx.relationship_to().relationship_definition().relationship_entity_name());
        var toField = getText(ctx.relationship_to().relationship_definition().relationship_field_name());
        var commentInTo = getText(ctx.relationship_to().relationship_javadoc());
        var toOptions = relationshipOptions(ctx.relationship_to().relationship_options().option());

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

    private Map<String, Object> relationshipOptions(List<ZdlParser.OptionContext> options) {
        return options.stream().collect(Collectors.toMap(o -> getText(o.option_name()), o -> getText(o.option_value(), true)));
    }

    @Override
    public void enterService_legacy(ZdlParser.Service_legacyContext ctx) {
        var serviceName = ctx.ID().getText();
        String serviceJavadoc = null; // getText(ctx.javadoc());
        var serviceAggregates = ctx.service_aggregates() != null? Arrays.asList(ctx.service_aggregates().getText().split(",")) : null;
        currentStack.push(new FluentMap()
                .with("name", serviceName)
                .with("isLegacy", true)
                .with("className", camelCase(serviceName))
                .with("javadoc", javadoc(serviceJavadoc))
                .with("comment", javadoc(serviceJavadoc))
                .with("aggregates", serviceAggregates)
                .with("methods", createCRUDMethods(serviceAggregates))
        );
        model.appendTo("services", serviceName, currentStack.peek());
    }

    @Override
    public void exitService_legacy(ZdlParser.Service_legacyContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterService(ZdlParser.ServiceContext ctx) {
        var serviceName = ctx.ID().getText();
        var serviceJavadoc = getText(ctx.javadoc());
        var serviceAggregates = ctx.service_aggregates() != null? Arrays.asList(ctx.service_aggregates().getText().split(",")) : null;
        currentStack.push(new FluentMap()
                .with("name", serviceName)
                .with("className", camelCase(serviceName))
                .with("javadoc", javadoc(serviceJavadoc))
                .with("comment", javadoc(serviceJavadoc))
                .with("aggregates", serviceAggregates)
                .with("methods", new FluentMap())
        );
        model.appendTo("services", serviceName, currentStack.peek());
    }

    @Override
    public void exitService(ZdlParser.ServiceContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterService_method(ZdlParser.Service_methodContext ctx) {
        var methodName = getText(ctx.service_method_name());
        var methodParamId = ctx.service_method_parameter_id() != null? "id" : null;
        var methodParameter = ctx.service_method_parameter() != null? ctx.service_method_parameter().getText() : null;
        var returnType = ctx.service_method_return() != null? ctx.service_method_return().ID().getText() : null;
        var returnTypeIsArray = ctx.service_method_return() != null? ctx.service_method_return().ARRAY() != null : null;
        var withEvents = getServiceMethodEvents(ctx.service_method_with_events());

        var method = new FluentMap()
                .with("name", methodName)
                .with("paramId", methodParamId)
                .with("parameter", methodParameter)
                .with("returnType", returnType)
                .with("returnTypeIsArray", returnTypeIsArray)
                .with("withEvents", withEvents)
                ;
        currentStack.peek().appendTo("methods", methodName, method);
        currentStack.push(method);
    }

    @Override
    public void exitService_method(ZdlParser.Service_methodContext ctx) {
        currentStack.pop();
    }

    private List<Object> getServiceMethodEvents(ZdlParser.Service_method_with_eventsContext ctx) {
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
    public void enterEvent(ZdlParser.EventContext ctx) {
        var name = ctx.event_name().getText();
        var channel = getText(ctx.event_channel());
        var javadoc = getText(ctx.javadoc());
        var kebabCase = kebabCase(name);
        currentStack.push(new FluentMap()
                .with("name", name)
                .with("channel", channel)
                .with("kebabCase", kebabCase)
                .with("javadoc", javadoc(javadoc))
                .with("fields", new FluentMap())
        );
        model.appendTo("events", name, currentStack.peek());
        currentCollection = "events";
    }

    @Override
    public void exitEvent(ZdlParser.EventContext ctx) {
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
    public void enterInput(ZdlParser.InputContext ctx) {
        var name = ctx.input_name().getText();
        var javadoc = getText(ctx.javadoc());
        currentStack.push(processEntity(name, javadoc, null));
        model.appendTo("inputs", name, currentStack.peek());
        currentCollection = "inputs";
    }

    @Override
    public void exitInput(ZdlParser.InputContext ctx) {
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
