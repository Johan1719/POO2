package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación extra para paginado/actualizaciones usadas por la UI.
 * Se dejó separada para no romper la estructura existente.
 */
public final class ClienteRepositoryPagination {

    private ClienteRepositoryPagination() {
    }

    public static List<Cliente> listarClientesPaginado(String celular, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        String cel = celular == null ? "" : celular.trim();

        String sql;
        boolean filtrar = !cel.isEmpty();
        if (filtrar) {
            sql = "SELECT IDCliente, FechaRegistro, NombreCliente, Nrocelular " +
                  "FROM Cliente " +
                  "WHERE Nrocelular = ? " +
                  "ORDER BY FechaRegistro DESC " +
                  "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        } else {
            sql = "SELECT IDCliente, FechaRegistro, NombreCliente, Nrocelular " +
                  "FROM Cliente " +
                  "ORDER BY FechaRegistro DESC " +
                  "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        }

        List<Cliente> result = new ArrayList<>();
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int idx = 1;
            if (filtrar) {
                ps.setString(idx++, cel);
            }
            ps.setInt(idx++, offset);
            ps.setInt(idx, pageSize);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Cliente c = new Cliente();
                    c.setIdCliente(rs.getString("IDCliente"));
                    Timestamp ts = rs.getTimestamp("FechaRegistro");
                    c.setFechaRegistro(ts != null ? ts.toLocalDateTime() : null);
                    c.setNombreCliente(rs.getString("NombreCliente"));
                    c.setNrocelular(rs.getString("Nrocelular"));
                    result.add(c);
                }
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar clientes paginado: " + e.getMessage(), e);
        }
    }

    public static int countClientesFiltrados(String celular) {
        String cel = celular == null ? "" : celular.trim();
        boolean filtrar = !cel.isEmpty();

        String sql;
        if (filtrar) {
            sql = "SELECT COUNT(*) AS total FROM Cliente WHERE Nrocelular = ?";
        } else {
            sql = "SELECT COUNT(*) AS total FROM Cliente";
        }

        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (filtrar) {
                ps.setString(1, cel);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar clientes filtrados: " + e.getMessage(), e);
        }

        return 0;
    }

    public static void actualizarTelefonoPorId(String idCliente, String nuevoCelular) {
        String sql = "UPDATE Cliente SET Nrocelular = ? WHERE IDCliente = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nuevoCelular);
            ps.setString(2, idCliente);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar telefono por ID: " + e.getMessage(), e);
        }
    }
}

