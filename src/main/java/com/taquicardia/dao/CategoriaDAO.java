package com.taquicardia.dao;

import com.taquicardia.modelo.Categoria;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Acceso a datos de categorías; no contiene lógica de presentación. */
public class CategoriaDAO {

    private Connection abrirConexion() throws SQLException {
        Connection conexion = ConexionBD.conectar();
        if (conexion == null) throw new SQLException("No fue posible conectar con la base de datos.");
        return conexion;
    }

    public List<Categoria> listar() throws SQLException {
        String sql = "SELECT c.id_categoria, c.nombre, c.descripcion, COUNT(p.id_producto) AS total_productos "
                + "FROM categorias c LEFT JOIN productos p ON p.id_categoria = c.id_categoria "
                + "GROUP BY c.id_categoria, c.nombre, c.descripcion ORDER BY c.nombre";
        List<Categoria> categorias = new ArrayList<>();
        try (Connection con = abrirConexion(); PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) categorias.add(mapear(rs));
        }
        return categorias;
    }

    public Categoria buscarPorId(int idCategoria) throws SQLException {
        String sql = "SELECT id_categoria, nombre, descripcion, 0 AS total_productos FROM categorias WHERE id_categoria = ?";
        try (Connection con = abrirConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public int insertar(Categoria categoria) throws SQLException {
        String sql = "INSERT INTO categorias (nombre, descripcion) VALUES (?, ?)";
        try (Connection con = abrirConexion(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categoria.getNombre().trim());
            ps.setString(2, valorNullable(categoria.getDescripcion()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se recibió el identificador de la categoría creada.");
                int id = keys.getInt(1);
                categoria.setIdCategoria(id);
                return id;
            }
        }
    }

    public boolean actualizar(Categoria categoria) throws SQLException {
        String sql = "UPDATE categorias SET nombre = ?, descripcion = ? WHERE id_categoria = ?";
        try (Connection con = abrirConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, categoria.getNombre().trim());
            ps.setString(2, valorNullable(categoria.getDescripcion()));
            ps.setInt(3, categoria.getIdCategoria());
            return ps.executeUpdate() == 1;
        }
    }

    /** Devuelve false si no existe la categoría o todavía tiene productos asignados. */
    public boolean eliminar(int idCategoria) throws SQLException {
        String sql = "DELETE FROM categorias WHERE id_categoria = ? "
                + "AND NOT EXISTS (SELECT 1 FROM productos WHERE id_categoria = ?)";
        try (Connection con = abrirConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCategoria);
            ps.setInt(2, idCategoria);
            return ps.executeUpdate() == 1;
        }
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(rs.getInt("id_categoria"));
        categoria.setNombre(rs.getString("nombre"));
        categoria.setDescripcion(rs.getString("descripcion"));
        categoria.setTotalProductos(rs.getInt("total_productos"));
        return categoria;
    }

    private String valorNullable(String texto) {
        return texto == null || texto.trim().isEmpty() ? null : texto.trim();
    }
}
