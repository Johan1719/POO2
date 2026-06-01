package com.laptitefrance.delivery.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    
    private static Properties props = new Properties();

    // BLOQUE ESTÁTICO: Se ejecuta UNA SOLA VEZ cuando el programa arranca.
    // Así evitamos leer el archivo del disco duro en cada consulta.
    static {
        try (FileInputStream in = new FileInputStream("src/main/resources/application.properties")) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("Error crítico al leer application.properties: " + e.getMessage());
        }
    }

    private DBConnection() {} // Constructor privado

    // AHORA ENTREGA UNA CONEXIÓN NUEVA CADA VEZ QUE SE LLAMA
    public static Connection getConexion() throws SQLException {
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String pass = props.getProperty("db.password");

        // DriverManager crea una conexión fresca, compatible con tu try-with-resources
        return DriverManager.getConnection(url, user, pass);
    }
}