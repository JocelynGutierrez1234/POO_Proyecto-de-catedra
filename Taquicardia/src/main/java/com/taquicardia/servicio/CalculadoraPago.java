package com.taquicardia.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.taquicardia.modelo.Factura;
import com.taquicardia.modelo.LineaFactura;
import com.taquicardia.modelo.MetodoPago;

// Cálculos de la cuenta: subtotal, IVA, propina, total y cambio
// Uso BigDecimal porque con double salen decimales raros
public class CalculadoraPago {

    public static final BigDecimal TASA_IVA = new BigDecimal("0.13");
    public static final BigDecimal TASA_PROPINA = new BigDecimal("0.10");

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    // 1. Subtotal = suma de cantidad x precio
    public BigDecimal calcularSubtotal(List<LineaFactura> lineas) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineaFactura linea : lineas) {
            subtotal = subtotal.add(linea.getImporte());
        }
        return redondear(subtotal);
    }

    // 2. Calcula IVA, propina (si la quiere) y total
    public void calcularTotales(Factura factura, boolean incluirPropina) {
        BigDecimal subtotal = calcularSubtotal(factura.getLineas());
        BigDecimal iva = redondear(subtotal.multiply(TASA_IVA));
        BigDecimal propina = incluirPropina ? redondear(subtotal.multiply(TASA_PROPINA)) : redondear(BigDecimal.ZERO);

        factura.setSubtotal(subtotal);
        factura.setIva(iva);
        factura.setPropina(propina);
        factura.setTotal(redondear(subtotal.add(iva).add(propina)));
    }

    // 3. Cambio (con tarjeta no hay cambio)
    public BigDecimal calcularCambio(MetodoPago metodo, BigDecimal total, BigDecimal montoRecibido) {
        if (metodo == MetodoPago.TARJETA) {
            return redondear(BigDecimal.ZERO);
        }
        if (montoRecibido == null || montoRecibido.compareTo(total) < 0) {
            throw new IllegalArgumentException("El monto recibido ($" + montoRecibido
                    + ") no alcanza para cubrir el total ($" + total + ").");
        }
        return redondear(montoRecibido.subtract(total));
    }
}
