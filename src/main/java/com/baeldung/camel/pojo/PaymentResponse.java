
package com.baeldung.camel.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "idFactura",
    "mensaje"
})
public class PaymentResponse {

    @JsonProperty("idFactura")
    private Integer idFactura;
    @JsonProperty("mensaje")
    private String mensaje;

    @JsonProperty("idFactura")
    public Integer getIdFactura() {
        return idFactura;
    }

    @JsonProperty("idFactura")
    public void setIdFactura(Integer idFactura) {
        this.idFactura = idFactura;
    }

    @JsonProperty("mensaje")
    public String getMensaje() {
        return mensaje;
    }

    @JsonProperty("mensaje")
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("idFactura", idFactura).append("mensaje", mensaje).toString();
    }

}
