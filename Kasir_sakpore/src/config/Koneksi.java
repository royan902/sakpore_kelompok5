/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author Acer Aspire Lite 15
 */
public class Koneksi {
    private static Connection conn;
    
    public static Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            try {
                // ganti sesuai username & password PostgreSQL kamu
                String url = "jdbc:postgresql://localhost:5432/kasir_sakpore";
                String user = "postgres";
                String pass = "muha8420";  
                
                conn = DriverManager.getConnection(url, user, pass);
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
        return conn;
    }
}
