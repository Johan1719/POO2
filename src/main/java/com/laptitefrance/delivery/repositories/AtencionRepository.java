package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Atencion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AtencionRepository implements IRepositorioBase<Atencion, String> {

    // ID = idCliente|codAsistente
    private static final String SEP = "|";

    @Override
    public void insert(Atencion entity) {
        String sql = "INSERT INTO Atencion (IDCliente, CodAsistente, FechaAtencion) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getIdCliente());
            ps.setString(2, entity.getCodAsistente());
            setTimestampOrNull(ps, 3, entity.getFechaAtencion());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Atencion: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Atencion> findById(String id) {
        String[] parts = splitId(id);
        String idCliente = parts[0];
        String codAsistente = parts[1];

        String sql = "SELECT IDCliente, CodAsistente, FechaAtencion FROM Atencion WHERE IDCliente = ? AND CodAsistente = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idCliente);
            ps.setString(2, codAsistente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Atencion a = new Atencion();
                a.setIdCliente(rs.getString("IDCliente"));
                a.setCodAsistente(rs.getString("CodAsistente"));
                a.setFechaAtencion(getTimestampAsLocalDateTime(rs, "FechaAtencion"));
                return Optional.of(a);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Atencion por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Atencion> findAll() {
        String sql = "SELECT IDCliente, CodAsistente, FechaAtencion FROM Atencion";
        List<Atencion> result = new ArrayList<>();
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Atencion a = new Atencion();
                a.setIdCliente(rs.getString("IDCliente"));
                a.setCodAsistente(rs.getString("CodAsistente"));
                a.setFechaAtencion(getTimestampAsLocalDateTime(rs, "FechaAtencion"));
                result.add(a);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Atenciones: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Atencion entity) {
        String sql = "UPDATE Atencion SET FechaAtencion = ? WHERE IDCliente = ? AND CodAsistente = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setTimestampOrNull(ps, 1, entity.getFechaAtencion());
            ps.setString(2, entity.getIdCliente());
            ps.setString(3, entity.getCodAsistente());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar Atencion: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        String[] parts = splitId(id);
        String idCliente = parts[0];
        String codAsistente = parts[1];

        String sql = "DELETE FROM Atencion WHERE IDCliente = ? AND CodAsistente = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idCliente);
            ps.setString(2, codAsistente);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Atencion: " + e.getMessage(), e);
        }
    }

    private static String[] splitId(String id) {
        if (id == null || !id.contains(SEP)) {
            throw new IllegalArgumentException("ID inválido para Atencion. Formato esperado: idCliente|codAsistente");
        }
        return id.split("\\|", 2);
    }

    private static void setTimestampOrNull(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) ps.setTimestamp(index, null);
        else ps.setTimestamp(index, Timestamp.valueOf(value));
    }

    private static LocalDateTime getTimestampAsLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }
}

