package com.baeldung.camel.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ProcessValidateAvailableServices implements Processor {

    public void process(Exchange exchange) throws Exception {
        String availableServices = exchange.getIn().getBody(String.class);
        boolean availableService = availableServices.contains(exchange.getIn()
                .getHeader("servicetype", String.class));
        exchange.setProperty("serviceAllowed", availableService);
    }
}
