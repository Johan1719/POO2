package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Direccion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DireccionRepository implements IRepositorioBase<Direccion, String> {

    // ID = ubicacion|distrito|areaLoc|idCliente
    private static final String SEP = "|";

    @Override
    public void insert(Direccion entity) {
        String sql = "INSERT INTO Direccion (Ubicacion, distrito, AreaLoc, IDCliente) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getUbicacion());
            ps.setString(2, entity.getDistrito());
            ps.setString(3, entity.getAreaLoc());
            ps.setString(4, entity.getIdCliente());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Direccion: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Direccion> findById(String id) {
        String[] parts = splitId(id);
        String ubicacion = parts[0];
        String distrito = parts[1];
        String areaLoc = parts[2];
        String idCliente = parts[3];

        String sql = "SELECT Ubicacion, distrito, AreaLoc, IDCliente FROM Direccion " +
                "WHERE Ubicacion = ? AND distrito = ? AND AreaLoc = ? AND IDCliente = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ubicacion);
            ps.setString(2, distrito);
            ps.setString(3, areaLoc);
            ps.setString(4, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Direccion d = new Direccion();
                d.setUbicacion(rs.getString("Ubicacion"));
                d.setDistrito(rs.getString("distrito"));
                d.setAreaLoc(rs.getString("AreaLoc"));
                d.setIdCliente(rs.getString("IDCliente"));
                return Optional.of(d);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Direccion por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Direccion> findAll() {
        String sql = "SELECT Ubicacion, distrito, AreaLoc, IDCliente FROM Direccion";
        List<Direccion> result = new ArrayList<>();
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Direccion d = new Direccion();
                d.setUbicacion(rs.getString("Ubicacion"));
                d.setDistrito(rs.getString("distrito"));
                d.setAreaLoc(rs.getString("AreaLoc"));
                d.setIdCliente(rs.getString("IDCliente"));
                result.add(d);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Direcciones: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Direccion entity) {
        String sql = "UPDATE Direccion SET distrito = ?, AreaLoc = ? WHERE Ubicacion = ? AND distrito = ? AND AreaLoc = ? AND IDCliente = ?";
        // Nota: como la llave primaria es compuesta, normalmente no conviene actualizar PK.
        // Aquí actualizamos únicamente columnas no PK (pero en este modelo todas son PK).
        // Para mantener consistencia, hacemos update por todos los campos actuales.
        // Si se requiere, se puede ajustar según uso real.
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // En la práctica, esto suele no cambiar nada; dejamos mapeo nulo/consistente.
            // Convertimos a parámetros en orden para que compile.
            ps.setString(1, entity.getDistrito());
            ps.setString(2, entity.getAreaLoc());
            ps.setString(3, entity.getUbicacion());
            ps.setString(4, entity.getDistrito());
            ps.setString(5, entity.getAreaLoc());
            ps.setString(6, entity.getIdCliente());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar Direccion: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        String[] parts = splitId(id);
        String ubicacion = parts[0];
        String distrito = parts[1];
        String areaLoc = parts[2];
        String idCliente = parts[3];

        String sql = "DELETE FROM Direccion WHERE Ubicacion = ? AND distrito = ? AND AreaLoc = ? AND IDCliente = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ubicacion);
            ps.setString(2, distrito);
            ps.setString(3, areaLoc);
            ps.setString(4, idCliente);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Direccion: " + e.getMessage(), e);
        }
    }

    private static String[] splitId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido para Direccion");
        }
        String[] parts = id.split("\\|", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("ID inválido para Direccion. Formato esperado: ubicacion|distrito|areaLoc|idCliente");
        }
        return parts;
    }
}

