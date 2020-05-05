package com.baeldung.camel;

import javax.ws.rs.core.MediaType;


import com.baeldung.camel.pojo.AccountBalance;
import com.baeldung.camel.pojo.InvoiceResponse;
import com.baeldung.camel.pojo.PaymentRequest;
import com.baeldung.camel.process.ProcessRequestUserValidation;
import com.baeldung.camel.process.ProcessServiceQuery;
import com.baeldung.camel.process.ProcessUserValidation;
import com.baeldung.camel.process.ProcessValidateAvailableServices;

import com.baeldung.camel.pojo.*;
import com.baeldung.camel.process.*;

import com.baeldung.camel.util.PredicateProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
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
import org.springframework.stereotype.Component;

import java.io.InputStream;

@SpringBootApplication
public class Application{

    private static final String USER_VALIDATION_URL = "http://localhost:9191/api/login/login";
    private static final String WATER_SERVICE_URL = "http://127.0.0.1:9090/servicios/pagos/v1/payments";
    private static final String AVAILABILITY_SERVICES_URL = "http://localhost:8082/services";
    private static final String VALIDATE_ACCOUNT_SERVICES_URL = "http://localhost:8088/users/12345678/validate";


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

            rest("aes/mod_val/v1").description("Teste REST Service").id("api-route")
                .post("/invoice")
                    .produces(MediaType.APPLICATION_JSON)
                    .consumes(MediaType.APPLICATION_JSON)
                    .bindingMode(RestBindingMode.json)
                    .type(PaymentRequest.class)
                    .enableCORS(true)
                    .to("direct:processPaymentService")
                .get("/invoice")
                    .bindingMode(RestBindingMode.json)
                    .outType(InvoiceResponse.class)
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
                                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                                    .endChoice()
                                .otherwise()
                                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
                                .endChoice()
                            .endChoice()
                    .otherwise()
                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
            .end();

            from("direct:postInvoiceService")
                .routeId("direct-routePost")
                .tracing()
                //.choice()
                    //.when(PredicateProcessor.isValidGetPredicate())
                        .to("direct:processUserValidation")
                            .choice()
                                .when(exchangeProperty("userValid").isEqualTo("true"))
                                .to("direct:processValidateAvailableServices")
                                    .choice()
                                        .when(exchangeProperty("serviceAllowed").isEqualTo(true))
                                            .to("direct:processValidateAccount")
                                                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                                        .otherwise()
                                            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                                        .endChoice()
                                .otherwise()
                                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
                                .endChoice()
                            .endChoice()
                    //.otherwise()
                       // .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                .end();


            from("direct:processUserValidation")
                .process(new ProcessRequestUserValidation())
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to(USER_VALIDATION_URL + "?user=${header.userName}&passw=${header.password}")
                .process(new ProcessUserValidation())
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
                    .unmarshal().json(JsonLibrary.Jackson, InvoiceResponse.class)
            .end();


            from("direct:processValidateAccount")
                .setHeader(Exchange.HTTP_URI, constant(VALIDATE_ACCOUNT_SERVICES_URL))
                    .to(VALIDATE_ACCOUNT_SERVICES_URL)
                    .unmarshal().json(JsonLibrary.Jackson, AccountBalance.class)
                    .process(new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            AccountBalance account = (AccountBalance) exchange.getIn().getBody();
                            exchange.setProperty("hasFounds",account.getMount()>0);
                        }
                    })

            from("direct:processPaymentService")
                .process(new CreatePaymentInvoiceProcessor())
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.HTTP_PATH, exchangeProperty("referenceInvoice"))
                .setHeader(Exchange.HTTP_URI, exchangeProperty("serviceQueryUrl"))
                .toD("${exchangeProperty.serviceQueryUrl}")
                    .unmarshal().json(JsonLibrary.Jackson, PaymentResponse.class)
            .end();
        }
    }
}
