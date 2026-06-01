package com.laptitefrance.delivery.views;

import com.laptitefrance.delivery.config.DBConnection;
import java.sql.Connection;
import java.sql.Statement;

public class MainApp {
    public static void main(String[] args) {
        System.out.println("Iniciando la Prueba Maestra de Integridad...");

        // Arreglo con la cadena exacta de inserciones respetando las dependencias (FK)
        String[] consultas = {
            // 1. Tablas Maestras
            "INSERT INTO Categoria (CodCat, NombreCat) VALUES ('CAT001', 'Postres')",
            "INSERT INTO Producto (CodProducto, NombreProd, Stock, PrecioProd, CodCat) VALUES ('PR001', 'Croissant', 50, 6.50, 'CAT001')",
            "INSERT INTO Tarifa (CodTarifa, NombreTarifa, PrecioTarifa) VALUES ('T01', 'Express', 5.00)",
            "INSERT INTO Pago (CodPago, MetodoPago, FechaPago, MontoTotal, Observaciones, CantDesc, IGV, CostoTarifa) VALUES ('PG001', 'Yape', GETDATE(), 18.00, 'Ninguna', 0, 2.74, 5.00)",
            
            // 2. Personal y Clientes
            "INSERT INTO Empleado (CodEmpleado, Nombre, Numero, Direccion, CorreoElec, AniosExp) VALUES ('E001', 'Jean Piere', '123', 'Surco', 'jean@mail.com', 2)",
            "INSERT INTO Repartidor (CodRepartidor) VALUES ('E001')",
            "INSERT INTO Cliente (IDCliente, FechaRegistro, NombreCliente, Nrocelular) VALUES ('C002', GETDATE(), 'Jazmin Mendoza', '987654321')",
            
            // 3. Tablas Transaccionales (El Pedido)
            "INSERT INTO Pedido (CodPedido, Fechasolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, CodRepartidor, IDCliente, CodTarifa, CodPago) VALUES ('PD001', GETDATE(), 18.00, 'EN CAMINO', GETDATE(), GETDATE(), 'E001', 'C002', 'T01', 'PG001')",
            "INSERT INTO Pedido_Producto (CodProducto, CodPedido, CantProd) VALUES ('PR001', 'PD001', 2)"
        };

        try (Connection con = DBConnection.getConexion();
             Statement stmt = con.createStatement()) {

            // Ejecutamos las consultas una por una
            for (int i = 0; i < consultas.length; i++) {
                stmt.executeUpdate(consultas[i]);
                System.out.println("Paso " + (i + 1) + " completado con éxito.");
            }

            System.out.println("\n¡ÉXITO TOTAL! Todas las tablas, llaves foráneas y la conexión funcionan en perfecta armonía.");

        } catch (Exception e) {
            System.err.println("\n❌ Error de Integridad detectado: " + e.getMessage());
            System.err.println("Revisa la tabla que menciona el error. Es probable que algún tipo de dato no coincida o falte una tabla.");
        }
    }
}