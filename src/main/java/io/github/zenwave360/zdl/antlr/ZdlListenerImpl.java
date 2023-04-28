package io.github.zenwave360.zdl.antlr;

import io.github.zenwave360.zdl.FluentMap;
import io.github.zenwave360.zdl.ZdlModel;
import io.github.zenwave360.zdl.lang.Inflector;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZdlListenerImpl extends ZdlBaseListener {

    ZdlModel model = new ZdlModel();
    FluentMap current = null;
    String currentCollection = null;

    Inflector inflector = Inflector.getInstance();

    public ZdlModel getModel() {
        return model;
    }

    @Override
    public void enterZdl(ZdlParser.ZdlContext ctx) {

    }

    private String getText(ParserRuleContext ctx) {
        return ctx != null? ctx.getText() : null;
    }

    private Object getText(ParserRuleContext ctx, Object defaultValue) {
        return ctx != null? ctx.getText() : defaultValue;
    }

    private String pluralize(String name) {
        return inflector.pluralize(name);
    }

    private String camelCase(String name) {
        return inflector.upperCamelCase(name);
    }

    private String lowerCamelCase(String name) {
        return inflector.lowerCamelCase(name);
    }
    private String kebabCase(String name) {
        return inflector.kebabCase(name);
    }

    private Object first(Object... args) {
        for(Object arg : args) {
            if(arg != null) {
                return arg;
            }
        }
        return null;
    }

    @Override
    public void enterGlobal_javadoc(ZdlParser.Global_javadocContext ctx) {
        var javadoc = getText(ctx);
        model.put("javadoc", javadoc);
    }

    @Override
    public void enterLegacy_constants(ZdlParser.Legacy_constantsContext ctx) {
        ctx.LEGACY_CONSTANT().stream().map(TerminalNode::getText).forEach(c -> model.appendTo("constants", c, c));
    }

    @Override
    public void enterEntity(ZdlParser.EntityContext ctx) {
        var name = ctx.entity_name().getText();
        var javadoc = getText(ctx.javadoc());
        var tableName = ctx.entity_table_name() != null? ctx.entity_table_name().ID().getText() : null;
        current = processEntity(name, javadoc, tableName);
        model.appendTo("entities", name, current);
        currentCollection = "entities";
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
                .with("javadoc", javadoc)
                .with("options", new FluentMap())
                .with("fields", new FluentMap())
        ;
    }

    @Override
    public void enterOption(ZdlParser.OptionContext ctx) {
        var name = ctx.reserved_option() != null? ctx.reserved_option().getText().replace("@", "") : getText(ctx.option_name());
        var value = getText(ctx.option_value(), true);
        current.appendTo("options", name, value);
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
        current.appendTo("fields", name, new FluentMap()
                .with("name", name)
                .with("type", type)
                .with("javadoc", javadoc)
                .with("comment", javadoc)
                .with("isEnum", isEnum)
                .with("isEntity", isEntity)
                .with("isArray", isArray)
                .with("options", new FluentMap())
                .with("validations", validations));
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
        current = processEntity(entityName, entityJavadoc, tableName);
        current.appendTo("options", "embedded", true);
        model.appendTo(currentCollection, entityName, current);
    }

    @Override
    public void enterEnum(ZdlParser.EnumContext ctx) {
        var name = getText(ctx.enum_name());
        var javadoc = getText(ctx.javadoc());
        current = new FluentMap()
                .with("name", name)
                .with("className", camelCase(name))
                .with("javadoc", javadoc)
                .with("comment", javadoc);
        ((FluentMap) model.get("enums")).appendTo("enums", name, current);
    }

    @Override
    public void enterEnum_value(ZdlParser.Enum_valueContext ctx) {
        var name = getText(ctx.enum_value_name());
        var javadoc = first(getText(ctx.javadoc(), getText(ctx.suffix_javadoc())));
        var value = getText(ctx.enum_value_value()); // TODO refactor when supporting quoted strings
        current.appendTo("values", name, new FluentMap()
                .with("name", name)
                .with("javadoc", javadoc)
                .with("comment", javadoc)
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
                .with("commentInFrom", commentInFrom)
                .with("commentInTo", commentInTo)
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
        current = new FluentMap()
                .with("name", serviceName)
                .with("className", camelCase(serviceName))
                .with("javadoc", serviceJavadoc)
                .with("comment", serviceJavadoc)
                .with("aggregates", serviceAggregates)
                .with("methods", createCRUDMethods(serviceAggregates))
                ;
        model.appendTo("services", serviceName, current);
    }

    public Map createCRUDMethods(List<String> entities) {
        var methods = new FluentMap();
        for (String entity : entities) {
            createCRUDMethods(entity.trim()).forEach(k -> methods.put((String) k.get("name"), k));
        }
        return methods;
    }

    @Override
    public void enterService(ZdlParser.ServiceContext ctx) {
        var serviceName = ctx.ID().getText();
        var serviceJavadoc = getText(ctx.javadoc());
        var serviceAggregates = ctx.service_aggregates() != null? Arrays.asList(ctx.service_aggregates().getText().split(",")) : null;
        current = new FluentMap()
                .with("name", serviceName)
                .with("className", camelCase(serviceName))
                .with("javadoc", serviceJavadoc)
                .with("comment", serviceJavadoc)
                .with("aggregates", serviceAggregates)
                .with("methods", new FluentMap())
                ;
        model.appendTo("services", serviceName, current);
    }

    @Override
    public void enterService_method(ZdlParser.Service_methodContext ctx) {
        var methodName = getText(ctx.service_method_name());
        var methodParamId = ctx.service_method_parameter_id() != null? "id" : null;
        var methodParameter = ctx.service_method_parameter() != null? ctx.service_method_parameter().getText() : null;
        var pageable = ctx.pageable() != null;
        var returnType = getText(ctx.service_method_return());
        var withEvents = getText(ctx.service_method_events()); // TODO split

        var method = new FluentMap()
                .with("name", methodName)
                .with("paramId", methodParamId)
                .with("parameter", methodParameter)
                .with("pageable", pageable)
                .with("returnType", returnType)
                .with("withEvents", withEvents)
                ;
        current.appendTo("methods", methodName, method);
    }

    @Override
    public void enterEvent(ZdlParser.EventContext ctx) {
        var name = ctx.event_name().getText();
        var javadoc = getText(ctx.javadoc());
        var kebabCase = kebabCase(name);
        current = new FluentMap()
                .with("name", name)
                .with("kebabCase", kebabCase)
                .with("javadoc", javadoc)
                .with("fields", new FluentMap())
                ;
        model.appendTo("events", name, current);
        currentCollection = "events";
    }

    @Override
    public void enterInput(ZdlParser.InputContext ctx) {
        var name = ctx.input_name().getText();
        var javadoc = getText(ctx.javadoc());
        current = processEntity(name, javadoc, null);
        model.appendTo("inputs", name, current);
        currentCollection = "inputs";
    }

    public List<Map> createCRUDMethods(String entity) {
        var crudMethods = new ArrayList<Map>();
        crudMethods.add(new FluentMap()
                .with("name", "get" + entity)
                .with("paramId", "id")
                .with("returnType", entity)
        );
        crudMethods.add(new FluentMap()
                .with("name", "list" + pluralize(entity))
                .with("pageable", true)
                .with("returnType", entity + "[]")
        );
        crudMethods.add(new FluentMap()
                .with("name", "create" + entity)
                .with("parameter", entity)
                .with("returnType", entity)
        );
        crudMethods.add(new FluentMap()
                .with("name", "update" + entity)
                .with("paramId", "id")
                .with("parameter", entity)
                .with("returnType", entity)
        );
        crudMethods.add(new FluentMap()
                .with("name", "delete" + entity)
                .with("paramId", "id")
        );
        return crudMethods;
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
