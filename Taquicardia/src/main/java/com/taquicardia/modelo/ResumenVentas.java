package com.taquicardia.modelo;

import java.io.Serializable;
import java.math.BigDecimal;

// Totales del historial de ventas (solo comprobantes vigentes)
public class ResumenVentas implements Serializable {
    private static final long serialVersionUID = 1L;

    private int cantidadFacturas;
    private int cantidadAnuladas;
    private BigDecimal totalSubtotal = BigDecimal.ZERO;
    private BigDecimal totalIva = BigDecimal.ZERO;
    private BigDecimal totalPropinas = BigDecimal.ZERO;
    private BigDecimal totalVendido = BigDecimal.ZERO;
    private BigDecimal totalEfectivo = BigDecimal.ZERO;
    private BigDecimal totalTarjeta = BigDecimal.ZERO;

    public ResumenVentas() {}

    public int getCantidadFacturas() { return cantidadFacturas; }
    public void setCantidadFacturas(int cantidadFacturas) { this.cantidadFacturas = cantidadFacturas; }
    public int getCantidadAnuladas() { return cantidadAnuladas; }
    public void setCantidadAnuladas(int cantidadAnuladas) { this.cantidadAnuladas = cantidadAnuladas; }
    public BigDecimal getTotalSubtotal() { return totalSubtotal; }
    public void setTotalSubtotal(BigDecimal totalSubtotal) { this.totalSubtotal = totalSubtotal; }
    public BigDecimal getTotalIva() { return totalIva; }
    public void setTotalIva(BigDecimal totalIva) { this.totalIva = totalIva; }
    public BigDecimal getTotalPropinas() { return totalPropinas; }
    public void setTotalPropinas(BigDecimal totalPropinas) { this.totalPropinas = totalPropinas; }
    public BigDecimal getTotalVendido() { return totalVendido; }
    public void setTotalVendido(BigDecimal totalVendido) { this.totalVendido = totalVendido; }
    public BigDecimal getTotalEfectivo() { return totalEfectivo; }
    public void setTotalEfectivo(BigDecimal totalEfectivo) { this.totalEfectivo = totalEfectivo; }
    public BigDecimal getTotalTarjeta() { return totalTarjeta; }
    public void setTotalTarjeta(BigDecimal totalTarjeta) { this.totalTarjeta = totalTarjeta; }
}
