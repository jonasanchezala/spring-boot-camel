package com.baeldung.camel.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ProcessServiceQuery implements Processor {


    private static final String WATER_SERVICE_URL = "http://127.0.0.1:9090/servicios/pagos/v1/payments";
    private static final String PHONE_SERVICE_URL = "http://localhost:8085/phoneBills";
    private static final String ENERGY_SERVICE_URL = "http://localhost:8085/electricityBills";
    private static final String WATER = "water";
    private static final String ENERGY = "energy";
    private static final String PHONE = "phone";

    public void process(Exchange exchange) throws Exception {

        String serviceType = exchange.getIn().getHeader("servicetype", String.class);
        String serviceQueryUrl = "";
        if (WATER.equals(serviceType)) {
            serviceQueryUrl = WATER_SERVICE_URL;
        } else if (ENERGY.equals(serviceType)) {
            serviceQueryUrl = ENERGY_SERVICE_URL;
        } else if (PHONE.equals(serviceType)) {
            serviceQueryUrl = PHONE_SERVICE_URL;
        }
        exchange.setProperty("serviceQueryUrl", serviceQueryUrl);
    }
}
