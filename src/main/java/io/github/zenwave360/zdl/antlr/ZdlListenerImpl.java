package io.github.zenwave360.zdl.antlr;

import io.github.zenwave360.zdl.FluentMap;
import io.github.zenwave360.zdl.ZdlModel;
import io.github.zenwave360.zdl.lang.Inflector;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;

public class ZdlListenerImpl extends ZdlBaseListener {

    ZdlModel model = new ZdlModel();
    FluentMap current = null;

    Inflector inflector = Inflector.getInstance();

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
    public void enterEntity(ZdlParser.EntityContext ctx) {
        var name = ctx.entity_name().getText();
        var javadoc = getText(ctx.javadoc());
        var tableName = ctx.entity_table_name() != null? ctx.entity_table_name().ID().getText() : null;
        processEntity(name, javadoc, tableName);
    }

    private void processEntity(String name, String javadoc, String tableName) {
        var className = camelCase(name);
        var instanceName = lowerCamelCase(className);
        var kebabCase = kebabCase(name);
        current = new FluentMap()
                .with("name", name)
                .with("className", className)
                .with("instanceName", instanceName)
                .with("classNamePlural", pluralize(name))
                .with("instanceNamePlural", pluralize(instanceName))
                .with("kebabCase", kebabCase)
                .with("kebabCasePlural", pluralize(kebabCase))
                .with("tableName", tableName)
                .with("javadoc", javadoc)
                .with("fields", new FluentMap())
        ;
        model.appendTo("entities", name, current);
    }

    @Override
    public void enterOption(ZdlParser.OptionContext ctx) {
        var name = ctx.SERVICE_OPTION() != null? "service" : getText(ctx.option_name());
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
        current.appendTo("fields", name, new FluentMap()
                .with("name", name)
                .with("type", type)
                .with("javadoc", javadoc)
                .with("comment", javadoc)
                .with("isEnum", isEnum)
                .with("isEntity", isEntity)
                .with("isArray", isArray)
                .with("validations", new ArrayList<>()));
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
        processEntity(entityName, entityJavadoc, tableName);
        current.appendTo("options", "embedded", true);
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
