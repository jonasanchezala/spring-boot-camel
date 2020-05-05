package com.baeldung.camel.process;


import com.baeldung.camel.pojo.AccountBalance;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ProcessAccountValidation implements Processor {
    public void process(Exchange exchange) throws Exception {
        AccountBalance account = (AccountBalance) exchange.getIn().getBody();
        exchange.setProperty("hasFounds",account.getMount()>0);
    }
}
