package com.taquicardia.modelo;

import java.math.BigDecimal;
import java.util.List;

// Totales para el cierre de caja
public class ResumenVentas {
    private int cantidadFacturas;
    private BigDecimal totalSubtotal = BigDecimal.ZERO;
    private BigDecimal totalIva = BigDecimal.ZERO;
    private BigDecimal totalPropinas = BigDecimal.ZERO;
    private BigDecimal totalVendido = BigDecimal.ZERO;
    private BigDecimal totalEfectivo = BigDecimal.ZERO;
    private BigDecimal totalTarjeta = BigDecimal.ZERO;

    public ResumenVentas(List<Factura> facturas) {
        for (Factura f : facturas) {
            cantidadFacturas++;
            totalSubtotal = totalSubtotal.add(f.getSubtotal());
            totalIva = totalIva.add(f.getIva());
            totalPropinas = totalPropinas.add(f.getPropina());
            totalVendido = totalVendido.add(f.getTotal());
            if (f.getPago() != null && f.getPago().getMetodoPago() == MetodoPago.TARJETA) {
                totalTarjeta = totalTarjeta.add(f.getTotal());
            } else {
                totalEfectivo = totalEfectivo.add(f.getTotal());
            }
        }
    }

    public int getCantidadFacturas() { return cantidadFacturas; }
    public BigDecimal getTotalSubtotal() { return totalSubtotal; }
    public BigDecimal getTotalIva() { return totalIva; }
    public BigDecimal getTotalPropinas() { return totalPropinas; }
    public BigDecimal getTotalVendido() { return totalVendido; }
    public BigDecimal getTotalEfectivo() { return totalEfectivo; }
    public BigDecimal getTotalTarjeta() { return totalTarjeta; }
}
