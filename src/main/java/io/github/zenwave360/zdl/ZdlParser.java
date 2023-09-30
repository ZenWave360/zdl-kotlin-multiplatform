package io.github.zenwave360.zdl;

import io.github.zenwave360.zdl.antlr.ZdlLexer;
import io.github.zenwave360.zdl.antlr.ZdlListenerImpl;
import io.github.zenwave360.zdl.antlr.ZdlModelPostProcessor;
import io.github.zenwave360.zdl.antlr.ZdlModelValidator;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ZdlParser {

    public static final List<String> STANDARD_FIELD_TYPES = List.of("String", "Integer", "Long", "BigDecimal", "Float", "Double", "Enum", "Boolean", "LocalDate", "ZonedDateTime", "Instant", "Duration", "UUID", "Blob", "AnyBlob", "ImageBlob", "TextBlob");

    public static Map<String, Object> parseModel(String model) throws IOException {
        CharStream zdl = CharStreams.fromString(model);
        ZdlLexer lexer = new ZdlLexer(zdl);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        io.github.zenwave360.zdl.antlr.ZdlParser parser = new io.github.zenwave360.zdl.antlr.ZdlParser(tokens);
        ParseTree tree = parser.zdl();
        ParseTreeWalker walker = new ParseTreeWalker();
        ZdlListenerImpl listener = new ZdlListenerImpl();
        walker.walk(listener, tree);
        var zdlModel = listener.getModel();
        zdlModel = ZdlModelPostProcessor.postProcess(zdlModel);
        try {
            zdlModel = ZdlModelValidator.validate(zdlModel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zdlModel;
    }
}
