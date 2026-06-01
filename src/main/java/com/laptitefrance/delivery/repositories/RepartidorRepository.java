package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Repartidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RepartidorRepository implements IRepositorioBase<Repartidor, String> {

    // Tabla dependiente: Repartidor(CodRepartidor FK -> Empleado)
    // En el repositorio devolvemos Repartidor con datos de Empleado.

    @Override
    public void insert(Repartidor entity) {
        String sql = "INSERT INTO Repartidor (CodRepartidor) VALUES (?)";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getCodEmpleado());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Repartidor: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Repartidor> findById(String id) {
        String sql = "SELECT e.CodEmpleado, e.Nombre, e.Numero, e.Direccion, e.CorreoElec, e.AniosExp " +
                "FROM Repartidor r INNER JOIN Empleado e ON r.CodRepartidor = e.CodEmpleado " +
                "WHERE r.CodRepartidor = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Repartidor r = new Repartidor();
                r.setCodEmpleado(rs.getString("CodEmpleado"));
                r.setNombre(rs.getString("Nombre"));
                r.setNumero(rs.getString("Numero"));
                r.setDireccion(rs.getString("Direccion"));
                r.setCorreoElec(rs.getString("CorreoElec"));
                r.setAniosExp(rs.getShort("AniosExp"));
                return Optional.of(r);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Repartidor por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Repartidor> findAll() {
        String sql = "SELECT e.CodEmpleado, e.Nombre, e.Numero, e.Direccion, e.CorreoElec, e.AniosExp " +
                "FROM Repartidor r INNER JOIN Empleado e ON r.CodRepartidor = e.CodEmpleado";
        List<Repartidor> result = new ArrayList<>();
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Repartidor r = new Repartidor();
                r.setCodEmpleado(rs.getString("CodEmpleado"));
                r.setNombre(rs.getString("Nombre"));
                r.setNumero(rs.getString("Numero"));
                r.setDireccion(rs.getString("Direccion"));
                r.setCorreoElec(rs.getString("CorreoElec"));
                r.setAniosExp(rs.getShort("AniosExp"));
                result.add(r);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Repartidores: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Repartidor entity) {
        // Como Repartidor es tabla dependiente por PK, actualizamos Empleado.
        String sql = "UPDATE Empleado SET Nombre = ?, Numero = ?, Direccion = ?, CorreoElec = ?, AniosExp = ? WHERE CodEmpleado = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getNombre());
            ps.setString(2, entity.getNumero());
            ps.setString(3, entity.getDireccion());
            ps.setString(4, entity.getCorreoElec());
            ps.setShort(5, entity.getAniosExp());
            ps.setString(6, entity.getCodEmpleado());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar Repartidor: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM Repartidor WHERE CodRepartidor = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Repartidor: " + e.getMessage(), e);
        }
    }
}

