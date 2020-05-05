package com.baeldung.camel;

import javax.ws.rs.core.MediaType;

import com.baeldung.camel.pojo.Invoice;
import com.baeldung.camel.process.ProcessServiceQuery;
import com.baeldung.camel.process.ProcessUserValidation;
import com.baeldung.camel.process.ProcessValidateAvailableServices;
import com.baeldung.camel.util.PredicateProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@SpringBootApplication
@ComponentScan(basePackages="com.baeldung.camel")
public class Application{

    private static final String USER_VALIDATION_URL = "http://localhost:9191/api/login/login";
    private static final String WATER_SERVICE_URL = "http://127.0.0.1:9090/servicios/pagos/v1/payments";
    private static final String AVAILABILITY_SERVICES_URL = "http://localhost:8082/services";

    private static final String GAS = "gas";
    private static final String WATER = "water";
    private static final String ENERGY = "energy";
    private static final String PHONE = "phone";

    private static final String QUERY_TRANSACTION_TYPE  = "query";
    private static final String PAYMENT_TRANSACTION_TYPE  = "payment";

    @Value("${server.port}")
    String serverPort;
    
    @Value("${baeldung.api.path}")
    String contextPath;
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    ServletRegistrationBean servletRegistrationBean() {
        ServletRegistrationBean servlet = new ServletRegistrationBean(new CamelHttpTransportServlet(), contextPath+"/*");
        servlet.setName("CamelServlet");
        return servlet;
    }


    @Component
    class RestApi extends RouteBuilder {

        @Override
        public void configure() {
            CamelContext context = new DefaultCamelContext();
            restConfiguration().contextPath(contextPath) //
                .port(serverPort)
                .enableCORS(true)
                .apiContextPath("/api-doc")
                .apiProperty("api.title", "Test REST API")
                .apiProperty("api.version", "v1")
                .apiProperty("cors", "true") // cross-site
                .apiContextRouteId("doc-api")
                .component("servlet")
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true");

            rest("/api/").description("Teste REST Service").id("api-route")
                .post("/invoice")
                    .produces(MediaType.APPLICATION_JSON)
                    .consumes(MediaType.APPLICATION_JSON)
                    .bindingMode(RestBindingMode.auto)
                    .type(Invoice.class)
                    .enableCORS(true)
                    .to("direct:postInvoiceService")
                .get("/invoice")
                    .bindingMode(RestBindingMode.json)
                    //.type(Invoice.class)
                    .to("direct:getInvoiceService");

            from("direct:getInvoiceService")
                .routeId("direct-routeGet")
                .tracing()
                .choice()
                    .when(PredicateProcessor.isValidGetPredicate())
                            .to("direct:processUserValidation")
                            .choice()
                                .when(exchangeProperty("userValid").isEqualTo("true"))
                                    .to("direct:processValidateAvailableServices")
                                    .choice()
                                    .when(exchangeProperty("serviceAllowed").isEqualTo(true))
                                        .to("direct:processServiceQuery")
                                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                                    .otherwise()
                                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(503))
                                    .endChoice()
                                .otherwise()
                                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
                                .endChoice()
                            .endChoice()
                    .otherwise()
                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
            .end();


            from("direct:processUserValidation")
                .setHeader(Exchange.HTTP_URI, constant(USER_VALIDATION_URL))
                .choice()
                    .when(header(Exchange.HTTP_METHOD).isEqualTo("GET"))
                        .setHeader(Exchange.HTTP_QUERY, simple("user=${header.userName}&passw=${header.password}"))
                        .to(USER_VALIDATION_URL)
                        .process(new ProcessUserValidation())
                    .otherwise()
                        .setHeader(Exchange.HTTP_QUERY, simple("user=${body.userName}&passw=${body.password}"))
                        .to(USER_VALIDATION_URL)
                        .process(new ProcessUserValidation())
                .endChoice()
            .end();

            from("direct:processValidateAvailableServices")
                .setHeader(Exchange.HTTP_URI, constant(AVAILABILITY_SERVICES_URL))
                    .to(AVAILABILITY_SERVICES_URL)
                        .process(new ProcessValidateAvailableServices())
            .end();

            from("direct:processServiceQuery")
                .process(new ProcessServiceQuery())
                .setHeader(Exchange.HTTP_PATH, simple("${header.reference}"))
                .setHeader(Exchange.HTTP_URI, exchangeProperty("serviceQueryUrl"))
                .toD("${exchangeProperty.serviceQueryUrl}")
                    .unmarshal().json(JsonLibrary.Jackson, Invoice.class)
                    .log("${body}")
            .end();
        }
    }
}
