package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Paginación real para el monitor de pedidos usando JOINs directos.
 * No depende de vw_MonitorPedidos.
 */
public final class PedidoMonitorRepositoryPagination {

    private PedidoMonitorRepositoryPagination() {
    }

    public static List<PedidoMonitorRow> listarPedidosPaginado(String estado, int page, int pageSize) {
        int p = Math.max(1, page);
        int ps = Math.max(1, pageSize);
        int offset = (p - 1) * ps;

        String est = (estado == null) ? "" : estado.trim();
        boolean filtrar = !est.isEmpty() && !est.equalsIgnoreCase("TODOS");

        // Traemos NombreCliente y NombreRepartidor. Si no hay repartidor => vacío.
        // Nota: en SQL Server no se puede combinar TOP con OFFSET/FETCH.
        String sql =
                "SELECT "+
                "  pr.CodPedido, "+
                "  pr.MontoPedido, "+
                "  pr.Estado, "+
                "  pr.Fechasolicitud, "+
                "  pr.TiempoEntEstimado, "+
                "  c.NombreCliente, "+
                "  e.Nombre AS NombreRepartidor "+
                "FROM Pedido pr "+
                "INNER JOIN Cliente c ON c.IDCliente = pr.IDCliente "+
                "LEFT JOIN Repartidor r ON r.CodRepartidor = pr.CodRepartidor "+
                "LEFT JOIN Empleado e ON e.CodEmpleado = r.CodRepartidor "+


                "WHERE 1=1 "+
                (filtrar ? " AND pr.Estado = ? " : " ") +
                "ORDER BY pr.Fechasolicitud DESC "+
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";


        List<PedidoMonitorRow> result = new ArrayList<>();

        try (Connection con = DBConnection.getConexion();
             PreparedStatement psStmt = con.prepareStatement(sql)) {

            int idx = 1;
            if (filtrar) {
                psStmt.setString(idx++, est);
            }
            psStmt.setInt(idx++, offset);
            psStmt.setInt(idx, ps);

            try (ResultSet rs = psStmt.executeQuery()) {
                while (rs.next()) {
                    PedidoMonitorRow row = new PedidoMonitorRow();
                    row.codPedido = rs.getString("CodPedido");
                    row.montoPedido = rs.getDouble("MontoPedido");
                    row.estado = rs.getString("Estado");
                    row.fechaSolicitud = getTimestampAsLocalDateTime(rs, "Fechasolicitud");
                    row.tiempoEntEstimado = getTimestampAsLocalDateTime(rs, "TiempoEntEstimado");
                    row.nombreCliente = rs.getString("NombreCliente");
                    row.nombreRepartidor = rs.getString("NombreRepartidor");
                    result.add(row);
                }
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar pedidos paginados (monitor): " + e.getMessage(), e);
        }
    }

    public static int contarPedidosFiltrados(String estado) {
        String est = (estado == null) ? "" : estado.trim();
        boolean filtrar = !est.isEmpty() && !est.equalsIgnoreCase("TODOS");

        String sql =
                "SELECT COUNT(*) AS total " +
                "FROM Pedido pr "+
                (filtrar ? " WHERE pr.Estado = ?" : "");

        try (Connection con = DBConnection.getConexion();
             PreparedStatement psStmt = con.prepareStatement(sql)) {

            if (filtrar) {
                psStmt.setString(1, est);
            }

            try (ResultSet rs = psStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }

            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar pedidos filtrados (monitor): " + e.getMessage(), e);
        }
    }

    private static LocalDateTime getTimestampAsLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    /** DTO interno para el monitor (fácil de mapear a la tabla). */
    public static class PedidoMonitorRow {
        public String codPedido;
        public double montoPedido;
        public String estado;
        public LocalDateTime fechaSolicitud;
        public LocalDateTime tiempoEntEstimado;
        public String nombreCliente;
        public String nombreRepartidor;
    }
}

