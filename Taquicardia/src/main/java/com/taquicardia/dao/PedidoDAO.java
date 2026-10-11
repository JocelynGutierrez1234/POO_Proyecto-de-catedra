package com.taquicardia.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import com.taquicardia.modelo.Pedido;

public class PedidoDAO {

    // 1. Crear un nuevo pedido en una mesa
    public boolean crearPedido(Pedido p) {
        String sql = "INSERT INTO pedidos (id_mesa, id_usuario, estado) VALUES (?, ?, 'PENDIENTE')";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, p.getIdMesa());
            ps.setInt(2, p.getIdUsuario());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Error al crear pedido en Taquicardia: " + e.getMessage());
            return false;
        }
    }

    // 2. Listar pedidos pendientes para la cocina 
    public List<Pedido> listarPedidosCocina() {
        List<Pedido> lista = new ArrayList<>();
        String sql = "SELECT * FROM pedidos WHERE estado IN ('PENDIENTE', 'EN_PREPARACION') ORDER BY fecha_hora ASC";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Pedido p = new Pedido();
                p.setIdPedido(rs.getInt("id_pedido"));
                p.setIdMesa(rs.getInt("id_mesa"));
                p.setIdUsuario(rs.getInt("id_usuario"));
                p.setFechaHora(rs.getTimestamp("fecha_hora"));
                p.setEstado(rs.getString("estado"));
                lista.add(p);
            }
        } catch (Exception e) {
            System.out.println("Error al listar pedidos en cocina: " + e.getMessage());
        }
        return lista;
    }

    // 3. Actualizar el estado del pedido 
    public boolean actualizarEstadoPedido(int idPedido, String nuevoEstado) {
        String sql = "UPDATE pedidos SET estado = ? WHERE id_pedido = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idPedido);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Error al cambiar estado del pedido: " + e.getMessage());
            return false;
        }
    }
}
