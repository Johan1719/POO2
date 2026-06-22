package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.dtos.PedidoRepartidorRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Consulta paginada de los pedidos asignados a un repartidor, con el nombre del
 * cliente (JOIN Cliente) y el método de pago (JOIN Pago). Sigue el patrón de
 * PedidoMonitorRepositoryPagination (estático, OFFSET/FETCH de SQL Server).
 */
public final class PedidoRepartidorRepositoryPagination {

    private PedidoRepartidorRepositoryPagination() {
    }

    public static List<PedidoRepartidorRow> listar(String codRepartidor, int page, int pageSize) {
        int p = Math.max(1, page);
        int ps = Math.max(1, pageSize);
        int offset = (p - 1) * ps;

        String sql =
                "SELECT p.CodPedido, c.NombreCliente, p.DireccionEntrega, pg.MetodoPago, p.Estado " +
                "FROM Pedido p " +
                "INNER JOIN Cliente c ON c.IDCliente = p.IDCliente " +
                "LEFT JOIN Pago pg ON pg.CodPago = p.CodPago " +
                "WHERE p.CodRepartidor = ? " +
                "ORDER BY p.Fechasolicitud DESC " +
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<PedidoRepartidorRow> result = new ArrayList<>();

        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps2 = con.prepareStatement(sql)) {

            ps2.setString(1, codRepartidor);
            ps2.setInt(2, offset);
            ps2.setInt(3, ps);

            try (ResultSet rs = ps2.executeQuery()) {
                while (rs.next()) {
                    PedidoRepartidorRow row = new PedidoRepartidorRow();
                    row.codPedido = rs.getString("CodPedido");
                    row.nombreCliente = rs.getString("NombreCliente");
                    row.direccionEntrega = rs.getString("DireccionEntrega");
                    row.metodoPago = rs.getString("MetodoPago");
                    row.estado = rs.getString("Estado");
                    result.add(row);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar pedidos del repartidor: " + e.getMessage(), e);
        }
    }

    public static int contar(String codRepartidor) {
        String sql = "SELECT COUNT(*) AS total FROM Pedido WHERE CodRepartidor = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codRepartidor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar pedidos del repartidor: " + e.getMessage(), e);
        }
    }
}
