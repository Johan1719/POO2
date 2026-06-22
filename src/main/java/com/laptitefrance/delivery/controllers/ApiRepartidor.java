package com.laptitefrance.delivery.controllers;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.dtos.PedidoMonitorRow;
import io.javalin.Javalin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * API simple (Javalin) para que un repartidor/empleado externo vea sus pedidos.
 */
public class ApiRepartidor {

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        }).start(8080);

        // GET: pedidos asignados a un repartidor (incluye dirección textual y cliente)
        app.get("/api/repartidores/{codRepartidor}/pedidos", ctx -> {
            String codRepartidor = ctx.pathParam("codRepartidor");
            if (codRepartidor == null || codRepartidor.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "codRepartidor inválido"));
                return;
            }

            // Mostrar SOLO pedidos pendientes del repartidor (definido como EN CAMINO)
            try {
                List<PedidoAsignadoApiRow> pedidos = listarPedidosPorRepartidor(codRepartidor.trim(), "EN CAMINO", 1, 1000);
                ctx.json(Map.of("codRepartidor", codRepartidor, "pedidos", pedidos));
            } catch (Throwable ex) {
                // Depuración: devolver el error exacto para corregir el 500.
                String msg = ex.getMessage();
                try {
                    if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                        msg = ex.getCause().getMessage();
                    }
                } catch (Exception ignore) {}

                ctx.status(500).json(Map.of(
                        "error", msg,
                        "estadoFiltro", "EN CAMINO",
                        "codRepartidor", codRepartidor,
                        "exType", ex.getClass().getName()
                ));
            }



        });

        // POST: marcar como entregado (pre-confirmación la hace el frontend)
        app.post("/api/repartidores/{codRepartidor}/pedidos/{codPedido}/entregar", ctx -> {
            String codRepartidor = ctx.pathParam("codRepartidor");
            String codPedido = ctx.pathParam("codPedido");

            if (codRepartidor == null || codRepartidor.trim().isEmpty() || codPedido == null || codPedido.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "codRepartidor/codPedido inválidos"));
                return;
            }

            // Validar que pertenece al repartidor
            boolean actualizado = marcarEntregadoSiPertenece(codPedido.trim(), codRepartidor.trim());
            if (!actualizado) {
                ctx.status(404).json(Map.of("error", "No existe el pedido o no pertenece al repartidor"));
                return;
            }

            ctx.json(Map.of("ok", true, "codPedido", codPedido.trim(), "estado", "ENTREGADO"));
        });

        System.out.println("✅ Servidor encendido en http://localhost:8080");
        System.out.println("✅ GET  /api/repartidores/{codRepartidor}/pedidos");
        System.out.println("✅ POST /api/repartidores/{codRepartidor}/pedidos/{codPedido}/entregar");
    }

    // ===== Implementación DB (MVP) =====

    private static List<PedidoAsignadoApiRow> listarPedidosPorRepartidor(
            String codRepartidor,
            String estadoFiltro,
            int page,
            int pageSize
    ) {
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
                        " c.IDCliente, " +
                        " c.NombreCliente " +
                        " FROM Pedido pr " +
                        " INNER JOIN Cliente c ON c.IDCliente = pr.IDCliente " +
                        " WHERE pr.CodRepartidor = ? " +
                        (filtrar ? " AND pr.Estado = ? " : " ") +
                        " ORDER BY pr.Fechasolicitud DESC " +
                        " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<PedidoAsignadoApiRow> result = new ArrayList<>();

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
                    PedidoAsignadoApiRow row = new PedidoAsignadoApiRow();
                    row.codPedido = rs.getString("CodPedido");
                    row.montoPedido = rs.getDouble("MontoPedido");
                    row.estado = rs.getString("Estado");
                    row.direccionEntrega = rs.getString("DireccionEntrega");
                    row.fechaSolicitud = getTimestampAsLocalDateTime(rs, "Fechasolicitud");
                    row.tiempoEntEstimado = getTimestampAsLocalDateTime(rs, "TiempoEntEstimado");

                    row.cliente = new ClienteApiRow();
                    row.cliente.idCliente = rs.getString("IDCliente");
                    row.cliente.nombreCliente = rs.getString("NombreCliente");

                    result.add(row);
                }
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar pedidos por repartidor: " + e.getMessage(), e);
        }
    }

    private static boolean marcarEntregadoSiPertenece(String codPedido, String codRepartidor) {
        // Ajusta strings si tu BD maneja otros valores.
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
        java.sql.Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    // ===== DTOs API internos =====

    public static class PedidoAsignadoApiRow {
        public String codPedido;
        public double montoPedido;
        public String estado;
        public String direccionEntrega;
        public LocalDateTime fechaSolicitud;
        public LocalDateTime tiempoEntEstimado;
        public ClienteApiRow cliente;
    }

    public static class ClienteApiRow {
        public String idCliente;
        public String nombreCliente;
    }
}

