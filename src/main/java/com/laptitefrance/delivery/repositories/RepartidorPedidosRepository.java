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

public class RepartidorPedidosRepository {

    public static final class PedidoRepartidorRow {
        public String codPedido;
        public double montoPedido;
        public String estado;
        public String direccionEntrega;
        public LocalDateTime fechaSolicitud;
        public LocalDateTime tiempoEntEstimado;

        public String nombreCliente;
        public String ubicacionEntrega; // alias semántico para la vista (viene de DireccionEntrega)
    }


    public List<PedidoRepartidorRow> listarPedidosPorRepartidor(String codRepartidor, String estadoFiltro, int page, int pageSize) {
        int p = Math.max(1, page);
        int ps = Math.max(1, pageSize);
        int offset = (p - 1) * ps;

        String est = (estadoFiltro == null) ? "" : estadoFiltro.trim();
        boolean filtrar = !est.isEmpty() && !est.equalsIgnoreCase("TODOS");

        String sql =
                "SELECT " +
                        " pr.CodPedido, " +
                        " pr.MontoPedido, " +
                        " pr.Estado, " +
                        " pr.Fechasolicitud, " +
                        " pr.TiempoEntEstimado, " +
                        " pr.DireccionEntrega, " +
                        " c.NombreCliente " +
                        " FROM Pedido pr " +
                        " INNER JOIN Cliente c ON c.IDCliente = pr.IDCliente " +
                        " WHERE pr.CodRepartidor = ? " +
                        (filtrar ? " AND pr.Estado = ? " : " ") +
                        " ORDER BY pr.Fechasolicitud DESC " +
                        " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<PedidoRepartidorRow> result = new ArrayList<>();

        try (Connection con = DBConnection.getConexion();
             PreparedStatement psStmt = con.prepareStatement(sql)) {

            int idx = 1;
            psStmt.setString(idx++, codRepartidor);
            if (filtrar) {
                psStmt.setString(idx++, est);
            }
            psStmt.setInt(idx++, offset);
            psStmt.setInt(idx, ps);

            try (ResultSet rs = psStmt.executeQuery()) {
                while (rs.next()) {
                    PedidoRepartidorRow row = new PedidoRepartidorRow();
                    row.codPedido = rs.getString("CodPedido");
                    row.montoPedido = rs.getDouble("MontoPedido");
                    row.estado = rs.getString("Estado");
                    row.direccionEntrega = rs.getString("DireccionEntrega");
                    row.ubicacionEntrega = row.direccionEntrega;
                    row.fechaSolicitud = getTimestampAsLocalDateTime(rs, "Fechasolicitud");
                    row.tiempoEntEstimado = getTimestampAsLocalDateTime(rs, "TiempoEntEstimado");
                    row.nombreCliente = rs.getString("NombreCliente");
                    result.add(row);
                }
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar pedidos por repartidor: " + e.getMessage(), e);
        }
    }

    public boolean marcarEntregadoSiPertenece(String codPedido, String codRepartidor) {
        String sql =
                "UPDATE Pedido " +
                        "SET Estado = 'ENTREGADO', " +
                        "    TiempoEntReal = ISNULL(TiempoEntReal, GETDATE()), " +
                        "    HoraEnvio = ISNULL(HoraEnvio, GETDATE()) " +
                        "WHERE CodPedido = ? AND CodRepartidor = ?";

        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codPedido);
            ps.setString(2, codRepartidor);

            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al marcar entregado: " + e.getMessage(), e);
        }
    }

    private static LocalDateTime getTimestampAsLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }
}

