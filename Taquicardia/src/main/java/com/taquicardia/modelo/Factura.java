package com.taquicardia.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Factura implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String VIGENTE = "VIGENTE";
    public static final String ANULADA = "ANULADA";

    private int idFactura;
    private String numeroFactura;
    private int idPedido;
    private int numeroMesa;
    private Integer idUsuario;
    private String nombreCliente = "Consumidor Final";
    private String documentoCliente;
    private Timestamp fechaHora;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal iva = BigDecimal.ZERO;
    private BigDecimal propina = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private String estado = VIGENTE;
    private String motivoAnulacion;
    private Timestamp fechaAnulacion;
    private List<LineaFactura> lineas = new ArrayList<>();
    private Pago pago;

    public Factura() {}

    public boolean isAnulada() { return ANULADA.equals(estado); }

    public int getIdFactura() { return idFactura; }
    public void setIdFactura(int idFactura) { this.idFactura = idFactura; }
    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }
    public int getIdPedido() { return idPedido; }
    public void setIdPedido(int idPedido) { this.idPedido = idPedido; }
    public int getNumeroMesa() { return numeroMesa; }
    public void setNumeroMesa(int numeroMesa) { this.numeroMesa = numeroMesa; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getDocumentoCliente() { return documentoCliente; }
    public void setDocumentoCliente(String documentoCliente) { this.documentoCliente = documentoCliente; }
    public Timestamp getFechaHora() { return fechaHora; }
    public void setFechaHora(Timestamp fechaHora) { this.fechaHora = fechaHora; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }
    public BigDecimal getPropina() { return propina; }
    public void setPropina(BigDecimal propina) { this.propina = propina; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }
    public Timestamp getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(Timestamp fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }
    public List<LineaFactura> getLineas() { return lineas; }
    public void setLineas(List<LineaFactura> lineas) { this.lineas = lineas; }
    public Pago getPago() { return pago; }
    public void setPago(Pago pago) { this.pago = pago; }
}
