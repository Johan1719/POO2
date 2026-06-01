package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Telefono;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TelefonoRepository implements IRepositorioBase<Telefono, Integer> {

    @Override
    public void insert(Telefono entity) {
        String sql = "INSERT INTO Telefono (CodEmpleado, NroTelefono) VALUES (?, ?)";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getCodEmpleado());
            ps.setString(2, entity.getNroTelefono());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Telefono: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Telefono> findById(Integer id) {
        String sql = "SELECT IDTelefono, CodEmpleado, NroTelefono FROM Telefono WHERE IDTelefono = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Telefono t = new Telefono();
                t.setIdTelefono(rs.getInt("IDTelefono"));
                t.setCodEmpleado(rs.getString("CodEmpleado"));
                t.setNroTelefono(rs.getString("NroTelefono"));
                return Optional.of(t);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Telefono por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Telefono> findAll() {
        String sql = "SELECT IDTelefono, CodEmpleado, NroTelefono FROM Telefono";
        List<Telefono> result = new ArrayList<>();
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Telefono t = new Telefono();
                t.setIdTelefono(rs.getInt("IDTelefono"));
                t.setCodEmpleado(rs.getString("CodEmpleado"));
                t.setNroTelefono(rs.getString("NroTelefono"));
                result.add(t);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Telefonos: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Telefono entity) {
        String sql = "UPDATE Telefono SET CodEmpleado = ?, NroTelefono = ? WHERE IDTelefono = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getCodEmpleado());
            ps.setString(2, entity.getNroTelefono());
            ps.setInt(3, entity.getIdTelefono());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar Telefono: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM Telefono WHERE IDTelefono = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Telefono: " + e.getMessage(), e);
        }
    }
}

