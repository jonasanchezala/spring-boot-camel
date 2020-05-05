package com.baeldung.camel.pojo;

import java.io.Serializable;

public class InvoiceResponse implements Serializable {
    private int idFactura;
    private double valorFactura;

    public int getIdFactura() {
        return idFactura;
    }

    public void setIdFactura(int idFactura) {
        this.idFactura = idFactura;
    }

    public double getValorFactura() {
        return valorFactura;
    }

    public void setValorFactura(double valorFactura) {
        this.valorFactura = valorFactura;
    }
}
