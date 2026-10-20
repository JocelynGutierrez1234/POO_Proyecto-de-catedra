package com.taquicardia.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.taquicardia.modelo.Factura;
import com.taquicardia.modelo.LineaFactura;
import com.taquicardia.modelo.MetodoPago;
import com.taquicardia.modelo.Pago;
import com.taquicardia.modelo.PedidoPorCobrar;

public class FacturaDAO {

    // tabla de productos (módulo de productos)
    private static final String TABLA_PRODUCTOS = "productos";
    private static final String COLUMNA_NOMBRE_PRODUCTO = "nombre";

    // estados del pedido y de la mesa
    private static final String ESTADO_LISTO = "LISTO";
    private static final String ESTADO_ENTREGADO = "ENTREGADO";
    private static final String MESA_LIBRE = "LIBRE";

    private static final int LIMITE_HISTORIAL = 500;

    // errores de MySQL: tabla o columna inexistente
    private static final int ER_NO_SUCH_TABLE = 1146;
    private static final int ER_BAD_FIELD = 1054;

    private Connection abrirConexion() throws SQLException {
        Connection con = ConexionBD.conectar();
        if (con == null) {
            throw new SQLException("No se pudo conectar a la base de datos.");
        }
        return con;
    }

    // se lee como LocalDateTime para que la hora sea la misma que guardó MySQL
    private static Timestamp leerFecha(ResultSet rs, String columna) throws SQLException {
        LocalDateTime fecha = rs.getObject(columna, LocalDateTime.class);
        return (fecha == null) ? null : Timestamp.valueOf(fecha);
    }

    // ---------- Pedidos por cobrar: LISTO o ENTREGADO, con productos y sin comprobante vigente ----------

    private static final String SQL_POR_COBRAR =
            "SELECT p.id_pedido, p.id_mesa, m.numero_mesa, p.fecha_hora, p.estado, "
          + "       COALESCE(SUM(d.cantidad), 0) AS articulos, "
          + "       COALESCE(SUM(d.cantidad * d.precio_unitario), 0) AS subtotal "
          + "FROM pedidos p "
          + "LEFT JOIN mesas m ON m.id_mesa = p.id_mesa "
          + "LEFT JOIN detalle_pedidos d ON d.id_pedido = p.id_pedido "
          + "WHERE p.estado IN (?, ?) "
          + "  AND NOT EXISTS (SELECT 1 FROM facturas f WHERE f.id_pedido = p.id_pedido AND f.estado = 'VIGENTE') ";

    private static final String SQL_POR_COBRAR_GROUP =
            "GROUP BY p.id_pedido, p.id_mesa, m.numero_mesa, p.fecha_hora, p.estado "
          + "HAVING COUNT(d.id_pedido) > 0 ";

    private PedidoPorCobrar mapearPorCobrar(ResultSet rs) throws SQLException {
        return PedidoPorCobrar.de(
                rs.getInt("id_pedido"),
                rs.getInt("id_mesa"),
                rs.getInt("numero_mesa"),
                leerFecha(rs, "fecha_hora"),
                rs.getString("estado"),
                rs.getInt("articulos"),
                rs.getBigDecimal("subtotal").setScale(2, RoundingMode.HALF_UP));
    }

    public List<PedidoPorCobrar> listarPedidosPorCobrar() throws SQLException {
        List<PedidoPorCobrar> lista = new ArrayList<>();
        String sql = SQL_POR_COBRAR + SQL_POR_COBRAR_GROUP + "ORDER BY p.fecha_hora ASC, p.id_pedido ASC";
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ESTADO_LISTO);
            ps.setString(2, ESTADO_ENTREGADO);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearPorCobrar(rs));
                }
            }
        }
        return lista;
    }

    public PedidoPorCobrar buscarPedidoPorCobrar(int idPedido) throws SQLException {
        String sql = SQL_POR_COBRAR + "AND p.id_pedido = ? " + SQL_POR_COBRAR_GROUP;
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ESTADO_LISTO);
            ps.setString(2, ESTADO_ENTREGADO);
            ps.setInt(3, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearPorCobrar(rs);
            }
        }
        return null;
    }

    // id del comprobante vigente del pedido, o null
    public Integer buscarIdFacturaPorPedido(int idPedido) throws SQLException {
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id_factura FROM facturas WHERE id_pedido = ? AND estado = 'VIGENTE'")) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    // ---------- Productos del pedido ----------

    public List<LineaFactura> listarLineas(int idPedido) throws SQLException {
        try (Connection con = abrirConexion()) {
            return listarLineas(con, idPedido);
        }
    }

    private List<LineaFactura> listarLineas(Connection con, int idPedido) throws SQLException {
        String conNombre =
                "SELECT d.id_producto, pr." + COLUMNA_NOMBRE_PRODUCTO + " AS nombre_producto, "
              + "       d.cantidad, d.precio_unitario "
              + "FROM detalle_pedidos d "
              + "LEFT JOIN " + TABLA_PRODUCTOS + " pr ON pr.id_producto = d.id_producto "
              + "WHERE d.id_pedido = ? ORDER BY d.id_detalle";
        try {
            return ejecutarLineas(con, conNombre, idPedido);
        } catch (SQLException e) {
            // si todavía no está la tabla de productos se muestra "Producto #id"
            if (e.getErrorCode() != ER_NO_SUCH_TABLE && e.getErrorCode() != ER_BAD_FIELD) throw e;
            String sinNombre =
                    "SELECT d.id_producto, NULL AS nombre_producto, d.cantidad, d.precio_unitario "
                  + "FROM detalle_pedidos d WHERE d.id_pedido = ? ORDER BY d.id_detalle";
            return ejecutarLineas(con, sinNombre, idPedido);
        }
    }

    private List<LineaFactura> ejecutarLineas(Connection con, String sql, int idPedido) throws SQLException {
        List<LineaFactura> lineas = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idProducto = rs.getInt("id_producto");
                    String nombre = rs.getString("nombre_producto");
                    if (nombre == null) nombre = "Producto #" + idProducto;
                    lineas.add(new LineaFactura(idProducto, nombre,
                            rs.getInt("cantidad"), rs.getBigDecimal("precio_unitario")));
                }
            }
        }
        return lineas;
    }

    // ---------- Registrar cobro (CREATE) ----------
    // comprobante + pago + pedido ENTREGADO + liberar mesa, todo en una sola transacción

    public int registrarCobro(Factura factura, Pago pago) throws SQLException, CobroException {
        if (factura == null || pago == null || pago.getMetodoPago() == null
                || factura.getSubtotal() == null || factura.getTotal() == null
                || pago.getMontoRecibido() == null || pago.getCambio() == null) {
            throw new IllegalArgumentException("Datos de cobro incompletos.");
        }
        final int idPedido = factura.getIdPedido();

        try (Connection con = abrirConexion()) {
            boolean confirmado = false;
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            con.setAutoCommit(false);
            try {
                // bloquea el pedido para que no se cobre dos veces al mismo tiempo
                int idMesa;
                boolean tieneMesa;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT estado, id_mesa FROM pedidos WHERE id_pedido = ? FOR UPDATE")) {
                    ps.setInt(1, idPedido);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new CobroException("El pedido #" + idPedido + " no existe.");
                        String estado = rs.getString("estado");
                        if (!ESTADO_LISTO.equals(estado) && !ESTADO_ENTREGADO.equals(estado)) {
                            throw new CobroException("El pedido #" + idPedido + " está en estado " + estado
                                    + "; solo se cobran pedidos LISTO o ENTREGADO.");
                        }
                        idMesa = rs.getInt("id_mesa");
                        tieneMesa = !rs.wasNull();
                    }
                }

                // bloquea la mesa (para liberar bien la mesa si se cobran varios pedidos de ella a la vez)
                if (tieneMesa) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT id_mesa FROM mesas WHERE id_mesa = ? FOR UPDATE")) {
                        ps.setInt(1, idMesa);
                        ps.executeQuery().close();
                    }
                }

                // ya tiene comprobante vigente
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT id_factura FROM facturas WHERE id_pedido = ? AND estado = 'VIGENTE'")) {
                    ps.setInt(1, idPedido);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new CobroException("El pedido #" + idPedido + " ya fue cobrado.", rs.getInt(1));
                        }
                    }
                }

                // la cuenta no debe haber cambiado desde que se calculó
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT COUNT(*), COALESCE(SUM(cantidad * precio_unitario), 0) "
                      + "FROM detalle_pedidos WHERE id_pedido = ?")) {
                    ps.setInt(1, idPedido);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) == 0) {
                            throw new CobroException("El pedido #" + idPedido + " no tiene productos; no se puede cobrar.");
                        }
                        BigDecimal subtotalBD = rs.getBigDecimal(2).setScale(2, RoundingMode.HALF_UP);
                        if (subtotalBD.compareTo(factura.getSubtotal()) != 0) {
                            throw new CobroException("La cuenta del pedido #" + idPedido
                                    + " cambió mientras se cobraba. Revisa el nuevo total y vuelve a confirmar.");
                        }
                    }
                }

                int idFactura;
                String sqlFactura = "INSERT INTO facturas (id_pedido, id_usuario, nombre_cliente, documento_cliente, "
                        + "subtotal, iva, propina, total) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sqlFactura, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idPedido);
                    if (factura.getIdUsuario() != null) ps.setInt(2, factura.getIdUsuario());
                    else ps.setNull(2, Types.INTEGER);
                    ps.setString(3, factura.getNombreCliente());
                    if (factura.getDocumentoCliente() != null) ps.setString(4, factura.getDocumentoCliente());
                    else ps.setNull(4, Types.VARCHAR);
                    ps.setBigDecimal(5, factura.getSubtotal());
                    ps.setBigDecimal(6, factura.getIva());
                    ps.setBigDecimal(7, factura.getPropina());
                    ps.setBigDecimal(8, factura.getTotal());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se obtuvo el id de la factura.");
                        idFactura = keys.getInt(1);
                    }
                }

                // número de comprobante FAC-000001
                String numero = String.format("FAC-%06d", idFactura);
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE facturas SET numero_factura = ? WHERE id_factura = ?")) {
                    ps.setString(1, numero);
                    ps.setInt(2, idFactura);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO pagos (id_factura, metodo_pago, monto_recibido, cambio) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, idFactura);
                    ps.setString(2, pago.getMetodoPago().name());
                    ps.setBigDecimal(3, pago.getMontoRecibido());
                    ps.setBigDecimal(4, pago.getCambio());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE pedidos SET estado = ? WHERE id_pedido = ?")) {
                    ps.setString(1, ESTADO_ENTREGADO);
                    ps.setInt(2, idPedido);
                    ps.executeUpdate();
                }

                // la mesa se libera si ya no tiene pedidos sin pagar
                if (tieneMesa) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT COUNT(*) FROM pedidos p "
                          + "WHERE p.id_mesa = ? AND NOT EXISTS (SELECT 1 FROM facturas f "
                          + "      WHERE f.id_pedido = p.id_pedido AND f.estado = 'VIGENTE')")) {
                        ps.setInt(1, idMesa);
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            if (rs.getInt(1) == 0) {
                                try (PreparedStatement up = con.prepareStatement(
                                        "UPDATE mesas SET estado = ? WHERE id_mesa = ?")) {
                                    up.setString(1, MESA_LIBRE);
                                    up.setInt(2, idMesa);
                                    up.executeUpdate();
                                }
                            }
                        }
                    }
                }

                con.commit();
                confirmado = true;
                factura.setIdFactura(idFactura);
                factura.setNumeroFactura(numero);
                return idFactura;

            } catch (SQLIntegrityConstraintViolationException e) {
                throw new CobroException("El pedido #" + idPedido + " ya fue cobrado.");
            } finally {
                // rollback antes de volver a autocommit, si no se guardaría lo que quedó a medias
                if (!confirmado) {
                    try { con.rollback(); } catch (SQLException ignorada) { }
                }
                try { con.setAutoCommit(true); } catch (SQLException ignorada) { }
            }
        }
    }

    // ---------- Consultar comprobantes (READ): JOIN facturas + pedidos + mesas + pagos ----------

    private static final String SQL_FACTURAS =
            "SELECT f.id_factura, f.numero_factura, f.id_pedido, f.id_usuario, f.nombre_cliente, "
          + "       f.documento_cliente, f.fecha_hora, f.subtotal, f.iva, f.propina, f.total, "
          + "       f.estado, f.motivo_anulacion, f.fecha_anulacion, "
          + "       p.id_mesa, m.numero_mesa, "
          + "       pg.id_pago, pg.metodo_pago, pg.monto_recibido, pg.cambio, pg.fecha_hora AS fecha_pago "
          + "FROM facturas f "
          + "INNER JOIN pedidos p ON p.id_pedido = f.id_pedido "
          + "LEFT JOIN mesas m ON m.id_mesa = p.id_mesa "
          + "LEFT JOIN pagos pg ON pg.id_factura = f.id_factura ";

    private Factura mapearFactura(ResultSet rs) throws SQLException {
        Factura f = new Factura();
        f.setIdFactura(rs.getInt("id_factura"));
        f.setNumeroFactura(rs.getString("numero_factura"));
        f.setIdPedido(rs.getInt("id_pedido"));
        int numeroMesa = rs.getInt("numero_mesa");
        f.setNumeroMesa(rs.wasNull() ? rs.getInt("id_mesa") : numeroMesa);
        int idUsuario = rs.getInt("id_usuario");
        f.setIdUsuario(rs.wasNull() ? null : idUsuario);
        f.setNombreCliente(rs.getString("nombre_cliente"));
        f.setDocumentoCliente(rs.getString("documento_cliente"));
        f.setFechaHora(leerFecha(rs, "fecha_hora"));
        f.setSubtotal(rs.getBigDecimal("subtotal"));
        f.setIva(rs.getBigDecimal("iva"));
        f.setPropina(rs.getBigDecimal("propina"));
        f.setTotal(rs.getBigDecimal("total"));
        f.setEstado(rs.getString("estado"));
        f.setMotivoAnulacion(rs.getString("motivo_anulacion"));
        f.setFechaAnulacion(leerFecha(rs, "fecha_anulacion"));

        String metodo = rs.getString("metodo_pago");
        if (metodo != null) {
            Pago pago = new Pago();
            pago.setIdPago(rs.getInt("id_pago"));
            pago.setIdFactura(f.getIdFactura());
            pago.setMetodoPago(MetodoPago.valueOf(metodo));
            pago.setMontoRecibido(rs.getBigDecimal("monto_recibido"));
            pago.setCambio(rs.getBigDecimal("cambio"));
            pago.setFechaHora(leerFecha(rs, "fecha_pago"));
            f.setPago(pago);
        }
        return f;
    }

    public Factura obtenerFactura(int idFactura) throws SQLException {
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(SQL_FACTURAS + "WHERE f.id_factura = ?")) {
            ps.setInt(1, idFactura);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Factura f = mapearFactura(rs);
                    f.setLineas(listarLineas(con, f.getIdPedido()));
                    return f;
                }
            }
        }
        return null;
    }

    // historial con filtros opcionales (null = sin filtro)
    public List<Factura> buscarFacturas(LocalDate desde, LocalDate hasta, String numero, String estado)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SQL_FACTURAS).append("WHERE 1 = 1 ");
        List<Object> parametros = new ArrayList<>();
        if (desde != null) {
            sql.append("AND f.fecha_hora >= ? ");
            parametros.add(desde.atStartOfDay());
        }
        if (hasta != null) {
            sql.append("AND f.fecha_hora < ? ");
            parametros.add(hasta.plusDays(1).atStartOfDay());
        }
        if (numero != null) {
            sql.append("AND f.numero_factura LIKE ? ");
            parametros.add("%" + numero + "%");
        }
        if (estado != null) {
            sql.append("AND f.estado = ? ");
            parametros.add(estado);
        }
        sql.append("ORDER BY f.fecha_hora DESC, f.id_factura DESC LIMIT ").append(LIMITE_HISTORIAL);

        List<Factura> lista = new ArrayList<>();
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFactura(rs));
            }
        }
        return lista;
    }

    // ---------- Editar datos del cliente (UPDATE) ----------
    // los importes no se modifican; solo nombre y documento de un comprobante vigente

    public void actualizarCliente(int idFactura, String nombreCliente, String documentoCliente)
            throws SQLException, CobroException {
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE facturas SET nombre_cliente = ?, documento_cliente = ? "
                   + "WHERE id_factura = ? AND estado = 'VIGENTE'")) {
            ps.setString(1, nombreCliente);
            if (documentoCliente != null) ps.setString(2, documentoCliente);
            else ps.setNull(2, Types.VARCHAR);
            ps.setInt(3, idFactura);
            if (ps.executeUpdate() == 0) {
                throw new CobroException("El comprobante no existe o está anulado; no se puede editar.");
            }
        }
    }

    // ---------- Anular comprobante (DELETE lógico) ----------
    // el registro y su pago se conservan; el pedido vuelve a quedar pendiente de cobro

    public void anularFactura(int idFactura, String motivo) throws SQLException, CobroException {
        try (Connection con = abrirConexion()) {
            boolean confirmado = false;
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT estado FROM facturas WHERE id_factura = ? FOR UPDATE")) {
                    ps.setInt(1, idFactura);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new CobroException("El comprobante no existe.");
                        if ("ANULADA".equals(rs.getString(1))) {
                            throw new CobroException("El comprobante ya estaba anulado.");
                        }
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE facturas SET estado = 'ANULADA', motivo_anulacion = ?, fecha_anulacion = NOW() "
                      + "WHERE id_factura = ?")) {
                    ps.setString(1, motivo);
                    ps.setInt(2, idFactura);
                    ps.executeUpdate();
                }
                con.commit();
                confirmado = true;
            } finally {
                if (!confirmado) {
                    try { con.rollback(); } catch (SQLException ignorada) { }
                }
                try { con.setAutoCommit(true); } catch (SQLException ignorada) { }
            }
        }
    }

    // fecha de hoy según MySQL (es la misma que usa DEFAULT CURRENT_TIMESTAMP)
    public LocalDate fechaActual() throws SQLException {
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement("SELECT CURDATE()");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getObject(1, LocalDate.class);
        }
    }
}
