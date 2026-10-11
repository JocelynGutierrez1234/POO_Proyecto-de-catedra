package com.taquicardia.dao;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConexionBD {
    // Apunta de forma provisional a la base de datos local de Taquicardia
    private static final String URL = "jdbc:mysql://localhost:3306/taquicardia_bd?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = ""; 

    public static Connection conectar() {
        Connection conexion = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conexion = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.out.println("Error de conexión provisional: " + e.getMessage());
        }
        return conexion;
    }
}
