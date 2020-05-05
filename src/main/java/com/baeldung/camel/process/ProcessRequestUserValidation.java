package com.baeldung.camel.process;

import com.baeldung.camel.pojo.PaymentRequest;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.io.InputStream;

public class ProcessRequestUserValidation implements Processor {

    private static final String USER_VALIDATION_URL = "http://localhost:9191/api/login/login";

    public void process(Exchange exchange) throws Exception {

        String transactionType = exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class);
        String userName = "";
        int password;

        if (!transactionType.equals("POST")){
            userName = exchange.getIn().getHeader("userName", String.class);
            password = Integer.parseInt(exchange.getIn().getHeader("password", String.class));
            exchange.setProperty("reference", exchange.getIn().getHeader("reference", String.class));
            exchange.setProperty("serviceType", exchange.getIn().getHeader("serviceType", String.class));
        }else{

            InputStream payment = exchange.getIn().getBody(InputStream.class);

            exchange.setProperty("payment", exchange.getIn().getBody(PaymentRequest.class).getUser());
            exchange.setProperty("password", exchange.getIn().getBody(PaymentRequest.class).getPassword());
            exchange.setProperty("channel", exchange.getIn().getBody(PaymentRequest.class).getChannel());
            exchange.setProperty("invoice", exchange.getIn().getBody(PaymentRequest.class).getInvoice());
            exchange.setProperty("reference", exchange.getIn().getBody(PaymentRequest.class).getInvoice().getReference());

            exchange.getIn().setHeader("serviceType", exchange.getIn().getBody(PaymentRequest.class)
                    .getInvoice().getType());
            userName = exchange.getIn().getBody(PaymentRequest.class).getUser();
            password = Integer.parseInt(exchange.getIn().getBody(PaymentRequest.class).getPassword());

            exchange.getIn().setBody(payment);
        }

        exchange.getIn().setHeader(Exchange.HTTP_URI, USER_VALIDATION_URL);
        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "user=" + userName + "&passw=" + password);
    }
}
