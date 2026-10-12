package com.taquicardia.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import com.taquicardia.modelo.Mesa;

public class MesaDAO {
    
    // Método para obtener todas las mesas 
    public List<Mesa> listarMesas() {
        List<Mesa> lista = new ArrayList<>();
        String sql = "SELECT * FROM mesas";
        
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Mesa m = new Mesa();
                m.setIdMesa(rs.getInt("id_mesa"));
                m.setNumeroMesa(rs.getInt("numero_mesa"));
                m.setCapacidad(rs.getInt("capacidad"));
                m.setEstado(rs.getString("estado"));
                lista.add(m);
            }
        } catch (Exception e) {
            System.out.println("Error al listar mesas en Taquicardia: " + e.getMessage());
        }
        return lista;
    }

    // Método para cambiar el estado de la mesa 
    public boolean cambiarEstadoMesa(int idMesa, String nuevoEstado) {
        String sql = "UPDATE mesas SET estado = ? WHERE id_mesa = ?";
        
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idMesa);
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Error al actualizar estado de mesa: " + e.getMessage());
            return false;
        }
    }
}
