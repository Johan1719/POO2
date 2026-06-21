package com.laptitefrance.delivery.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.dtos.RepartidorMonitorRow;

public class RepartidorMonitorRepository {



    public List<RepartidorMonitorRow> listarRepartidoresConEstado() {
        String sql = "SELECT " +
                "  r.CodRepartidor, " +
                "  e.Nombre AS NombreRepartidor, " +
                "  SUM(CASE WHEN p.Estado = 'EN CAMINO' THEN 1 ELSE 0 END) AS PedidosEnCamino, " +
                "  SUM(CASE WHEN p.Estado = 'ENTREGADO' THEN 1 ELSE 0 END) AS PedidosEntregados, " +
                "  CASE " +
                "    WHEN SUM(CASE WHEN p.Estado = 'EN CAMINO' THEN 1 ELSE 0 END) > 0 THEN 'OCUPADO' " +
                "    ELSE 'DISPONIBLE' " +
                "  END AS EstadoRepartidor " +
                "FROM Repartidor r " +
                "JOIN Empleado e ON e.CodEmpleado = r.CodRepartidor " +
                "LEFT JOIN Pedido p ON p.CodRepartidor = r.CodRepartidor " +
                "GROUP BY r.CodRepartidor, e.Nombre " +
                "ORDER BY EstadoRepartidor DESC, e.Nombre ASC";

        List<RepartidorMonitorRow> result = new ArrayList<>();

        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RepartidorMonitorRow row = new RepartidorMonitorRow();
                    row.codRepartidor = rs.getString("CodRepartidor");
                    row.nombreRepartidor = rs.getString("NombreRepartidor");
                    row.pedidosEnCamino = rs.getInt("PedidosEnCamino");
                    row.pedidosEntregados = rs.getInt("PedidosEntregados");
                    row.estado = rs.getString("EstadoRepartidor");
                    result.add(row);
                }
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar repartidores con estado: " + e.getMessage(), e);
        }
    }
}

