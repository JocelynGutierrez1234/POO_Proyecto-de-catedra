package com.taquicardia.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.taquicardia.modelo.Factura;
import com.taquicardia.modelo.LineaFactura;
import com.taquicardia.modelo.MetodoPago;
import com.taquicardia.modelo.Pago;
import com.taquicardia.modelo.PedidoPorCobrar;

public class FacturaDAO {

    // Tabla de Carlos (cambiar si le pone otro nombre)
    private static final String TABLA_PRODUCTOS = "productos";
    private static final String COLUMNA_NOMBRE_PRODUCTO = "nombre";

    // Estados que define Helen
    private static final String ESTADO_LISTO = "LISTO";
    private static final String ESTADO_ENTREGADO = "ENTREGADO";
    private static final String MESA_LIBRE = "LIBRE";

    private Connection abrirConexion() throws SQLException {
        Connection con = ConexionBD.conectar();
        if (con == null) {
            throw new SQLException("No se pudo conectar a la base de datos (revisen ConexionBD).");
        }
        return con;
    }

    // Pedidos listos que no tienen factura
    private static final String SQL_POR_COBRAR =
            "SELECT p.id_pedido, p.id_mesa, m.numero_mesa, p.fecha_hora, p.estado, "
          + "       COALESCE(SUM(d.cantidad), 0) AS articulos, "
          + "       COALESCE(SUM(d.cantidad * d.precio_unitario), 0) AS subtotal "
          + "FROM pedidos p "
          + "LEFT JOIN mesas m ON m.id_mesa = p.id_mesa "
          + "LEFT JOIN detalle_pedidos d ON d.id_pedido = p.id_pedido "
          + "LEFT JOIN facturas f ON f.id_pedido = p.id_pedido "
          + "WHERE p.estado IN ('" + ESTADO_LISTO + "', '" + ESTADO_ENTREGADO + "') "
          + "  AND f.id_factura IS NULL ";

    private static final String SQL_POR_COBRAR_GROUP =
            "GROUP BY p.id_pedido, p.id_mesa, m.numero_mesa, p.fecha_hora, p.estado ";

    private PedidoPorCobrar mapearPorCobrar(ResultSet rs) throws SQLException {
        return PedidoPorCobrar.de(
                rs.getInt("id_pedido"),
                rs.getInt("id_mesa"),
                rs.getInt("numero_mesa"),
                rs.getTimestamp("fecha_hora"),
                rs.getString("estado"),
                rs.getInt("articulos"),
                rs.getBigDecimal("subtotal").setScale(2, java.math.RoundingMode.HALF_UP));
    }

    // 1. Listar pedidos por cobrar
    public List<PedidoPorCobrar> listarPedidosPorCobrar() {
        List<PedidoPorCobrar> lista = new ArrayList<>();
        String sql = SQL_POR_COBRAR + SQL_POR_COBRAR_GROUP + "ORDER BY p.fecha_hora ASC";
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearPorCobrar(rs));
            }
        } catch (Exception e) {
            System.out.println("Error al listar pedidos por cobrar: " + e.getMessage());
        }
        return lista;
    }

    // 2. Buscar un pedido por cobrar
    public PedidoPorCobrar buscarPedidoPorCobrar(int idPedido) {
        String sql = SQL_POR_COBRAR + "AND p.id_pedido = ? " + SQL_POR_COBRAR_GROUP;
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearPorCobrar(rs);
            }
        } catch (Exception e) {
            System.out.println("Error al buscar pedido por cobrar: " + e.getMessage());
        }
        return null;
    }

    // 3. Productos del pedido
    public List<LineaFactura> listarLineas(int idPedido) {
        try (Connection con = abrirConexion()) {
            return listarLineas(con, idPedido);
        } catch (Exception e) {
            System.out.println("Error al listar líneas del pedido: " + e.getMessage());
            return new ArrayList<>();
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
            // por si todavía no está la tabla de productos
            System.out.println("Aviso: no se pudo leer la tabla " + TABLA_PRODUCTOS + " (" + e.getMessage() + ")");
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

    // 4. Guardar el cobro (factura, pago, pedido entregado y liberar mesa)
    // Va en una transacción para que si algo falla no se guarde a medias
    public int registrarCobro(Factura factura, Pago pago) throws SQLException {
        Connection con = abrirConexion();
        try {
            con.setAutoCommit(false);

            // revisar que el pedido esté listo y no se haya cobrado
            int idMesa;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT estado, id_mesa FROM pedidos WHERE id_pedido = ? FOR UPDATE")) {
                ps.setInt(1, factura.getIdPedido());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("El pedido #" + factura.getIdPedido() + " no existe.");
                    String estado = rs.getString("estado");
                    if (!ESTADO_LISTO.equals(estado) && !ESTADO_ENTREGADO.equals(estado)) {
                        throw new SQLException("El pedido #" + factura.getIdPedido()
                                + " está en estado " + estado + "; solo se cobran pedidos LISTOS.");
                    }
                    idMesa = rs.getInt("id_mesa");
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id_factura FROM facturas WHERE id_pedido = ?")) {
                ps.setInt(1, factura.getIdPedido());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) throw new SQLException("El pedido #" + factura.getIdPedido() + " ya fue cobrado.");
                }
            }

            // guardar factura
            int idFactura;
            String sqlFactura = "INSERT INTO facturas (id_pedido, id_usuario, nombre_cliente, documento_cliente, "
                    + "subtotal, iva, propina, total) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sqlFactura, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, factura.getIdPedido());
                if (factura.getIdUsuario() != null) ps.setInt(2, factura.getIdUsuario());
                else ps.setNull(2, Types.INTEGER);
                ps.setString(3, factura.getNombreCliente());
                ps.setString(4, factura.getDocumentoCliente());
                ps.setBigDecimal(5, factura.getSubtotal());
                ps.setBigDecimal(6, factura.getIva());
                ps.setBigDecimal(7, factura.getPropina());
                ps.setBigDecimal(8, factura.getTotal());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    idFactura = keys.getInt(1);
                }
            }

            // numero de factura FAC-000001
            String numero = String.format("FAC-%06d", idFactura);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE facturas SET numero_factura = ? WHERE id_factura = ?")) {
                ps.setString(1, numero);
                ps.setInt(2, idFactura);
                ps.executeUpdate();
            }

            // guardar pago
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO pagos (id_factura, metodo_pago, monto_recibido, cambio) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, idFactura);
                ps.setString(2, pago.getMetodoPago().name());
                ps.setBigDecimal(3, pago.getMontoRecibido());
                ps.setBigDecimal(4, pago.getCambio());
                ps.executeUpdate();
            }

            // pedido entregado
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE pedidos SET estado = ? WHERE id_pedido = ?")) {
                ps.setString(1, ESTADO_ENTREGADO);
                ps.setInt(2, factura.getIdPedido());
                ps.executeUpdate();
            }

            // si la mesa ya no debe nada se libera
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM pedidos p LEFT JOIN facturas f ON f.id_pedido = p.id_pedido "
                  + "WHERE p.id_mesa = ? AND f.id_factura IS NULL")) {
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

            con.commit();
            factura.setIdFactura(idFactura);
            factura.setNumeroFactura(numero);
            return idFactura;

        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException ignorada) { }
            throw e;
        } finally {
            try { con.setAutoCommit(true); con.close(); } catch (SQLException ignorada) { }
        }
    }

    // Facturas con su pago
    private static final String SQL_FACTURAS =
            "SELECT f.*, m.numero_mesa, pg.id_pago, pg.metodo_pago, pg.monto_recibido, pg.cambio, "
          + "       pg.fecha_hora AS fecha_pago "
          + "FROM facturas f "
          + "JOIN pedidos p ON p.id_pedido = f.id_pedido "
          + "LEFT JOIN mesas m ON m.id_mesa = p.id_mesa "
          + "LEFT JOIN pagos pg ON pg.id_factura = f.id_factura ";

    private Factura mapearFactura(ResultSet rs) throws SQLException {
        Factura f = new Factura();
        f.setIdFactura(rs.getInt("id_factura"));
        f.setNumeroFactura(rs.getString("numero_factura"));
        f.setIdPedido(rs.getInt("id_pedido"));
        f.setNumeroMesa(rs.getInt("numero_mesa"));
        int idUsuario = rs.getInt("id_usuario");
        f.setIdUsuario(rs.wasNull() ? null : idUsuario);
        f.setNombreCliente(rs.getString("nombre_cliente"));
        f.setDocumentoCliente(rs.getString("documento_cliente"));
        f.setFechaHora(rs.getTimestamp("fecha_hora"));
        f.setSubtotal(rs.getBigDecimal("subtotal"));
        f.setIva(rs.getBigDecimal("iva"));
        f.setPropina(rs.getBigDecimal("propina"));
        f.setTotal(rs.getBigDecimal("total"));

        String metodo = rs.getString("metodo_pago");
        if (metodo != null) {
            Pago pago = new Pago();
            pago.setIdPago(rs.getInt("id_pago"));
            pago.setIdFactura(f.getIdFactura());
            pago.setMetodoPago(MetodoPago.valueOf(metodo));
            pago.setMontoRecibido(rs.getBigDecimal("monto_recibido"));
            pago.setCambio(rs.getBigDecimal("cambio"));
            pago.setFechaHora(rs.getTimestamp("fecha_pago"));
            f.setPago(pago);
        }
        return f;
    }

    // 5. Obtener una factura
    public Factura obtenerFactura(int idFactura) {
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
        } catch (Exception e) {
            System.out.println("Error al obtener factura: " + e.getMessage());
        }
        return null;
    }

    // 6. Facturas de un día (cierre de caja)
    public List<Factura> listarFacturasPorFecha(LocalDate fecha) {
        List<Factura> lista = new ArrayList<>();
        String sql = SQL_FACTURAS + "WHERE DATE(f.fecha_hora) = ? ORDER BY f.fecha_hora ASC";
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFactura(rs));
            }
        } catch (Exception e) {
            System.out.println("Error al listar facturas: " + e.getMessage());
        }
        return lista;
    }
}
