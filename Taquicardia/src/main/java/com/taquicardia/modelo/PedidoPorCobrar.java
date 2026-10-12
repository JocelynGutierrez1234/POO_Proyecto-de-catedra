package com.taquicardia.modelo;

import java.math.BigDecimal;
import java.sql.Timestamp;

// Pedido listo que todavía no se ha pagado (hereda de Pedido)
public class PedidoPorCobrar extends Pedido {
    private int numeroMesa;
    private int totalArticulos;
    private BigDecimal subtotal;

    // Constructor vacío
    public PedidoPorCobrar() {}

    public int getNumeroMesa() { return numeroMesa; }
    public void setNumeroMesa(int numeroMesa) { this.numeroMesa = numeroMesa; }
    public int getTotalArticulos() { return totalArticulos; }
    public void setTotalArticulos(int totalArticulos) { this.totalArticulos = totalArticulos; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    // Lo uso en el DAO para llenar el objeto
    public static PedidoPorCobrar de(int idPedido, int idMesa, int numeroMesa, Timestamp fecha,
                                     String estado, int articulos, BigDecimal subtotal) {
        PedidoPorCobrar p = new PedidoPorCobrar();
        p.setIdPedido(idPedido);
        p.setIdMesa(idMesa);
        p.setNumeroMesa(numeroMesa);
        p.setFechaHora(fecha);
        p.setEstado(estado);
        p.setTotalArticulos(articulos);
        p.setSubtotal(subtotal);
        return p;
    }
}
