package com.taquicardia.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class Pago implements Serializable {
    private static final long serialVersionUID = 1L;

    private int idPago;
    private int idFactura;
    private MetodoPago metodoPago;
    private BigDecimal montoRecibido;
    private BigDecimal cambio;
    private Timestamp fechaHora;

    // Constructor vacío
    public Pago() {}

    public int getIdPago() { return idPago; }
    public void setIdPago(int idPago) { this.idPago = idPago; }
    public int getIdFactura() { return idFactura; }
    public void setIdFactura(int idFactura) { this.idFactura = idFactura; }
    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }
    public BigDecimal getMontoRecibido() { return montoRecibido; }
    public void setMontoRecibido(BigDecimal montoRecibido) { this.montoRecibido = montoRecibido; }
    public BigDecimal getCambio() { return cambio; }
    public void setCambio(BigDecimal cambio) { this.cambio = cambio; }
    public Timestamp getFechaHora() { return fechaHora; }
    public void setFechaHora(Timestamp fechaHora) { this.fechaHora = fechaHora; }
}
