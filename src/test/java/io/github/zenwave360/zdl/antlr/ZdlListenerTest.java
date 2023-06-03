package io.github.zenwave360.zdl.antlr;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zenwave360.zdl.ZdlModel;
import io.github.zenwave360.zdl.ZdlParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ZdlListenerTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parseZdl_Simple() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/simple.zdl");

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_NestedFields() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/nested-fields.zdl");

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_NestedId_Inputs_Outputs() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/nested-input-output-model.zdl");

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_UnrecognizedTokens() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/unrecognized-tokens.zdl");

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }


    private static ZdlModel parseZdl(String fileName) throws IOException {
        CharStream zdl = CharStreams.fromFileName(fileName);
        return (ZdlModel) ZdlParser.parseModel(zdl.toString());
    }

}
