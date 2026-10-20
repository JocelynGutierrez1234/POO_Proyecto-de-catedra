package com.taquicardia.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.taquicardia.modelo.Factura;
import com.taquicardia.modelo.LineaFactura;
import com.taquicardia.modelo.MetodoPago;
import com.taquicardia.modelo.ResumenVentas;

// Cálculos de la cuenta: subtotal, IVA, propina, total, cambio y resumen de ventas
public class CalculadoraPago {

    public static final BigDecimal TASA_IVA = new BigDecimal("0.13");
    public static final BigDecimal TASA_PROPINA = new BigDecimal("0.10");

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularSubtotal(List<LineaFactura> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("El pedido no tiene productos; no se puede cobrar.");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineaFactura linea : lineas) {
            if (linea.getCantidad() <= 0 || linea.getPrecioUnitario() == null
                    || linea.getPrecioUnitario().signum() < 0) {
                throw new IllegalArgumentException("El producto \"" + linea.getNombreProducto()
                        + "\" tiene una cantidad o un precio inválido.");
            }
            subtotal = subtotal.add(linea.getImporte());
        }
        return redondear(subtotal);
    }

    public void calcularTotales(Factura factura, boolean incluirPropina) {
        BigDecimal subtotal = calcularSubtotal(factura.getLineas());
        BigDecimal iva = redondear(subtotal.multiply(TASA_IVA));
        BigDecimal propina = incluirPropina ? redondear(subtotal.multiply(TASA_PROPINA)) : redondear(BigDecimal.ZERO);

        factura.setSubtotal(subtotal);
        factura.setIva(iva);
        factura.setPropina(propina);
        factura.setTotal(redondear(subtotal.add(iva).add(propina)));
    }

    // con tarjeta no hay cambio
    public BigDecimal calcularCambio(MetodoPago metodo, BigDecimal total, BigDecimal montoRecibido) {
        if (metodo == null) {
            throw new IllegalArgumentException("Selecciona un método de pago válido.");
        }
        if (metodo == MetodoPago.TARJETA) {
            return redondear(BigDecimal.ZERO);
        }
        if (montoRecibido == null || montoRecibido.compareTo(total) < 0) {
            throw new IllegalArgumentException("El monto recibido ($" + montoRecibido
                    + ") no alcanza para cubrir el total ($" + total + ").");
        }
        return redondear(montoRecibido.subtract(total));
    }

    // los comprobantes anulados no suman
    public ResumenVentas resumir(List<Factura> facturas) {
        ResumenVentas r = new ResumenVentas();
        for (Factura f : facturas) {
            if (f.isAnulada()) {
                r.setCantidadAnuladas(r.getCantidadAnuladas() + 1);
                continue;
            }
            r.setCantidadFacturas(r.getCantidadFacturas() + 1);
            r.setTotalSubtotal(r.getTotalSubtotal().add(f.getSubtotal()));
            r.setTotalIva(r.getTotalIva().add(f.getIva()));
            r.setTotalPropinas(r.getTotalPropinas().add(f.getPropina()));
            r.setTotalVendido(r.getTotalVendido().add(f.getTotal()));
            if (f.getPago() != null) {
                if (f.getPago().getMetodoPago() == MetodoPago.TARJETA) {
                    r.setTotalTarjeta(r.getTotalTarjeta().add(f.getTotal()));
                } else if (f.getPago().getMetodoPago() == MetodoPago.EFECTIVO) {
                    r.setTotalEfectivo(r.getTotalEfectivo().add(f.getTotal()));
                }
            }
        }
        return r;
    }
}
