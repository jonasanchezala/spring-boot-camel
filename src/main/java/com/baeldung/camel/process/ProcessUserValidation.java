package com.baeldung.camel.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ProcessUserValidation implements Processor {
    public void process(Exchange exchange) throws Exception {
        exchange.setProperty("userValid", exchange.getIn().getBody(String.class));
    }
}
