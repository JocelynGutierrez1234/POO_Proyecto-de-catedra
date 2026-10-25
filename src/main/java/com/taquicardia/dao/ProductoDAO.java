package com.taquicardia.dao;

import com.taquicardia.modelo.Producto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Consultas JDBC para el catálogo de platillos y bebidas. */
public class ProductoDAO {

    public enum ResultadoEliminacion { ELIMINADO, ARCHIVADO, NO_ENCONTRADO }

    private Connection abrirConexion() throws SQLException {
        Connection conexion = ConexionBD.conectar();
        if (conexion == null) throw new SQLException("No fue posible conectar con la base de datos.");
        return conexion;
    }

    public List<Producto> listar(String busqueda, String tipo, Integer idCategoria,
                                 String disponibilidad, String estado) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT p.id_producto, p.id_categoria, c.nombre AS categoria_nombre, "
                + "p.nombre, p.descripcion, p.tipo, p.precio, p.disponible, p.activo "
                + "FROM productos p JOIN categorias c ON c.id_categoria = p.id_categoria WHERE 1 = 1 ");
        List<Object> parametros = new ArrayList<>();
        if (busqueda != null && !busqueda.isBlank()) {
            sql.append("AND (p.nombre LIKE ? OR p.descripcion LIKE ?) ");
            String patron = "%" + busqueda.trim() + "%";
            parametros.add(patron);
            parametros.add(patron);
        }
        if ("PLATILLO".equals(tipo) || "BEBIDA".equals(tipo)) {
            sql.append("AND p.tipo = ? ");
            parametros.add(tipo);
        }
        if (idCategoria != null && idCategoria > 0) {
            sql.append("AND p.id_categoria = ? ");
            parametros.add(idCategoria);
        }
        if ("si".equals(disponibilidad)) sql.append("AND p.disponible = 1 ");
        else if ("no".equals(disponibilidad)) sql.append("AND p.disponible = 0 ");
        if ("activos".equals(estado)) sql.append("AND p.activo = 1 ");
        else if ("inactivos".equals(estado)) sql.append("AND p.activo = 0 ");
        sql.append("ORDER BY p.activo DESC, c.nombre, p.nombre");

        List<Producto> productos = new ArrayList<>();
        try (Connection con = abrirConexion(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) ps.setObject(i + 1, parametros.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) productos.add(mapear(rs));
            }
        }
        return productos;
    }

    public Producto buscarPorId(int idProducto) throws SQLException {
        String sql = "SELECT p.id_producto, p.id_categoria, c.nombre AS categoria_nombre, p.nombre, p.descripcion, "
                + "p.tipo, p.precio, p.disponible, p.activo FROM productos p "
                + "JOIN categorias c ON c.id_categoria = p.id_categoria WHERE p.id_producto = ?";
        try (Connection con = abrirConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public int insertar(Producto producto) throws SQLException {
        String sql = "INSERT INTO productos (id_categoria, nombre, descripcion, tipo, precio, disponible, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = abrirConexion(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            asignar(ps, producto);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se recibió el identificador del producto creado.");
                int id = keys.getInt(1);
                producto.setIdProducto(id);
                return id;
            }
        }
    }

    public boolean actualizar(Producto producto) throws SQLException {
        String sql = "UPDATE productos SET id_categoria = ?, nombre = ?, descripcion = ?, tipo = ?, precio = ?, "
                + "disponible = ?, activo = ? WHERE id_producto = ?";
        try (Connection con = abrirConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            asignar(ps, producto);
            ps.setInt(8, producto.getIdProducto());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean cambiarDisponibilidad(int idProducto, boolean disponible) throws SQLException {
        String sql = "UPDATE productos SET disponible = ? WHERE id_producto = ? AND activo = 1";
        try (Connection con = abrirConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, disponible);
            ps.setInt(2, idProducto);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Elimina físicamente un producto nuevo. Si ya aparece en detalle_pedidos,
     * lo archiva y lo marca no disponible para preservar recibos e historial.
     */
    public ResultadoEliminacion eliminar(int idProducto) throws SQLException {
        try (Connection con = abrirConexion()) {
            boolean autoCommitAnterior = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                boolean referenciado;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT 1 FROM detalle_pedidos WHERE id_producto = ? LIMIT 1")) {
                    ps.setInt(1, idProducto);
                    try (ResultSet rs = ps.executeQuery()) { referenciado = rs.next(); }
                }

                ResultadoEliminacion resultado;
                if (referenciado) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE productos SET activo = 0, disponible = 0 WHERE id_producto = ?")) {
                        ps.setInt(1, idProducto);
                        resultado = ps.executeUpdate() == 1 ? ResultadoEliminacion.ARCHIVADO
                                : ResultadoEliminacion.NO_ENCONTRADO;
                    }
                } else {
                    try (PreparedStatement ps = con.prepareStatement("DELETE FROM productos WHERE id_producto = ?")) {
                        ps.setInt(1, idProducto);
                        resultado = ps.executeUpdate() == 1 ? ResultadoEliminacion.ELIMINADO
                                : ResultadoEliminacion.NO_ENCONTRADO;
                    }
                }
                con.commit();
                return resultado;
            } catch (SQLException e) {
                try { con.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            } finally {
                try { con.setAutoCommit(autoCommitAnterior); } catch (SQLException ignorada) { }
            }
        }
    }

    private void asignar(PreparedStatement ps, Producto producto) throws SQLException {
        ps.setInt(1, producto.getIdCategoria());
        ps.setString(2, producto.getNombre().trim());
        String descripcion = producto.getDescripcion() == null ? "" : producto.getDescripcion().trim();
        ps.setString(3, descripcion.isEmpty() ? null : descripcion);
        ps.setString(4, producto.getTipo().trim().toUpperCase());
        ps.setBigDecimal(5, producto.getPrecio());
        ps.setBoolean(6, producto.isDisponible());
        ps.setBoolean(7, producto.isActivo());
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto producto = new Producto();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setIdCategoria(rs.getInt("id_categoria"));
        producto.setCategoriaNombre(rs.getString("categoria_nombre"));
        producto.setNombre(rs.getString("nombre"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setTipo(rs.getString("tipo"));
        producto.setPrecio(rs.getBigDecimal("precio"));
        producto.setDisponible(rs.getBoolean("disponible"));
        producto.setActivo(rs.getBoolean("activo"));
        return producto;
    }
}
