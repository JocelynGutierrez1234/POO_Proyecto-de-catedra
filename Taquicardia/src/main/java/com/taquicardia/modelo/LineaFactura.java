package com.taquicardia.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

// Cada producto que sale en la cuenta/factura
public class LineaFactura implements Serializable {
    private static final long serialVersionUID = 1L;

    private int idProducto;
    private String nombreProducto;
    private int cantidad;
    private BigDecimal precioUnitario;

    // Constructor vacío
    public LineaFactura() {}

    public LineaFactura(int idProducto, String nombreProducto, int cantidad, BigDecimal precioUnitario) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    // cantidad x precio
    public BigDecimal getImporte() {
        if (precioUnitario == null) return BigDecimal.ZERO;
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
}
