package io.github.zenwave360.zdl;

import io.github.zenwave360.zdl.antlr.ZdlLexer;
import io.github.zenwave360.zdl.antlr.ZdlListenerImpl;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.util.Map;

public class ZdlParser {

    public static Map<String, Object> parseModel(String model) throws IOException {
        CharStream zdl = CharStreams.fromString(model);
        ZdlLexer lexer = new ZdlLexer(zdl);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        io.github.zenwave360.zdl.antlr.ZdlParser parser = new io.github.zenwave360.zdl.antlr.ZdlParser(tokens);
        ParseTree tree = parser.zdl();
        ParseTreeWalker walker = new ParseTreeWalker();
        ZdlListenerImpl listener = new ZdlListenerImpl();
        walker.walk(listener, tree);
        return listener.getModel();
    }
}
