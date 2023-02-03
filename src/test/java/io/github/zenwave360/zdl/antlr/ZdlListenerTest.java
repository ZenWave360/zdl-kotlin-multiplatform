package io.github.zenwave360.zdl.antlr;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zenwave360.zdl.ZdlParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.github.zenwave360.zdl.antlr.JSONPath.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ZdlListenerTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parseZdl_SuffixJavadoc() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/suffix_javadoc.zdl");

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_CompleteZdl() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/complete.zdl");
        assertEquals("ZenWave Online Food Delivery - Orders Module.", get(model, "$.javadoc"));

        // CONFIG
        assertEquals("io.zenwave360.example.orders", get(model, "$.config.basePackage"));
        assertEquals("mongodb", get(model, "$.config.persistence"));

        // APIS
        assertEquals(3, get(model, "$.apis", Map.of()).size());
        assertEquals("asyncapi", get(model, "$.apis.default.type"));
        assertEquals("provider", get(model, "$.apis.default.role"));
        assertEquals("orders/src/main/resources/apis/asyncapi.yml", get(model, "$.apis.default.config.uri"));
        assertEquals("asyncapi", get(model, "$.apis.RestaurantsAsyncAPI.type"));
        assertEquals("client", get(model, "$.apis.RestaurantsAsyncAPI.role"));
        assertEquals("restaurants/src/main/resources/apis/asyncapi.yml", get(model, "$.apis.RestaurantsAsyncAPI.config.uri"));

        // ENTITIES
        assertEquals(5, get(model, "$.entities", Map.of()).size());
        assertEquals("CustomerOrder", get(model, "$.entities.CustomerOrder.name"));
        assertEquals("customer_order", get(model, "$.entities.CustomerOrder.tableName"));
        assertEquals("customer-orders", get(model, "$.entities.CustomerOrder.kebabCasePlural"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.options.aggregate"));
        assertNull(get(model, "$.entities.CustomerOrder.javadoc"));

        assertEquals(5, get(model, "$.entities.CustomerOrder.fields", Map.of()).size());
        assertEquals("Instant", get(model, "$.entities.CustomerOrder.fields.orderTime.type"));
        assertEquals("Instant.now()", get(model, "$.entities.CustomerOrder.fields.orderTime.initialValue"));
        assertNotNull(get(model, "$.entities.CustomerOrder.fields.orderTime.validations.required"));
        assertEquals("orderTime javadoc", get(model, "$.entities.CustomerOrder.fields.orderTime.javadoc"));
        assertEquals("orderTime javadoc", get(model, "$.entities.CustomerOrder.fields.orderTime.comment"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.orderTime.isEnum"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.orderTime.isEntity"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.orderTime.isArray"));

        assertEquals("OrderStatus", get(model, "$.entities.CustomerOrder.fields.status.type"));
        assertEquals("OrderStatus.RECEIVED", get(model, "$.entities.CustomerOrder.fields.status.initialValue"));
        assertNotNull(get(model, "$.entities.CustomerOrder.fields.status.validations.required"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.status.isEnum"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.status.isEntity"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.status.isArray"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.status.isComplexType"));

        assertEquals("Customer", get(model, "$.entities.CustomerOrder.fields.customerDetails.type"));
        assertNull(get(model, "$.entities.CustomerOrder.fields.customerDetails.initialValue"));
        assertNull(get(model, "$.entities.CustomerOrder.fields.customerDetails.validations.required"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.customerDetails.isEnum"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.customerDetails.isEntity"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.customerDetails.isArray"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.customerDetails.isComplexType"));
        assertNotNull(get(model, "$.entities.CustomerOrder.fields.customerDetails.options.auditing"));
        assertNotNull(get(model, "$.entities.CustomerOrder.fields.customerDetails.options.ref"));

        assertEquals("OrderItem", get(model, "$.entities.CustomerOrder.fields.orderItems.type"));
        assertNull(get(model, "$.entities.CustomerOrder.fields.orderItems.initialValue"));
        assertNull(get(model, "$.entities.CustomerOrder.fields.orderItems.validations.required"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.orderItems.isEnum"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.orderItems.isEntity"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.orderItems.isArray"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.orderItems.isComplexType"));
        assertEquals("orderItems javadoc", get(model, "$.entities.CustomerOrder.fields.orderItems.javadoc"));
        assertEquals("orderItems javadoc", get(model, "$.entities.CustomerOrder.fields.orderItems.comment"));

        // RELATIONSHIPS
        assertEquals("Address", get(model, "$.relationships.ManyToOne.ManyToOne_Address{customer}_Customer.from"));
        assertEquals("Customer", get(model, "$.relationships.ManyToOne.ManyToOne_Address{customer}_Customer.to"));
        assertEquals("Address.customer javadoc", get(model, "$.relationships.ManyToOne.ManyToOne_Address{customer}_Customer.commentInFrom"));
        assertEquals("customer", get(model, "$.relationships.ManyToOne.ManyToOne_Address{customer}_Customer.injectedFieldInFrom"));
        assertNull(get(model, "$.relationships.ManyToOne.ManyToOne_Address{customer}_Customer.injectedFieldInTo"));

        assertEquals("Address.customer javadoc", get(model, "$.relationships.OneToMany.OneToMany_Customer{addresses}_Address{customer}.commentInTo"));

        assertEquals("Customer", get(model, "$.relationships.OneToOne.OneToOne_Customer{address}_@IdAddress{customer}.from"));
        assertEquals(true, get(model, "$.relationships.OneToOne.OneToOne_Customer{address}_@IdAddress{customer}.toOptions.Id"));


        // SERVICES
        assertEquals(1, get(model, "$.services", Map.of()).size());
        assertEquals(List.of("CustomerOrder"), get(model, "$.services.OrdersService.aggregates"));
        assertEquals(7, get(model, "$.services.OrdersService.methods", Map.of()).size());

        assertEquals(List.of("OrderEvent", "OrderStatusUpdated"), get(model, "$.services.OrdersService.methods.updateKitchenStatus.withEvents"));
        assertEquals("RestaurantsAsyncAPI", get(model, "$.services.OrdersService.methods.updateKitchenStatus.options.asyncapi.api"));
        assertEquals("KitchenOrdersStatusChannel", get(model, "$.services.OrdersService.methods.updateKitchenStatus.options.asyncapi.channel"));
        assertEquals(1, get(model, "$.services.OrdersService.methods.updateKitchenStatus.optionsList", List.of()).size());

        assertEquals(2, get(model, "$.services.OrdersService.methods.cancelOrder.options", Map.of()).size());
        assertEquals(2, get(model, "$.services.OrdersService.methods.cancelOrder.optionsList", List.of()).size());

        assertEquals("/search", get(model, "$.services.OrdersService.methods.searchOrders.options.post.path"));
        assertEquals("String", get(model, "$.services.OrdersService.methods.searchOrders.options.post.params.param1"));

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_Simple() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/simple.zdl");

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_Problems() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/customer-address-problems.zdl");

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model.get("problems")));
    }

    @Test
    public void parseZdl_Policies() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/policies.zdl");

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
