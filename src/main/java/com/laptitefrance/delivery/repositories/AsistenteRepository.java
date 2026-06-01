package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Asistente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AsistenteRepository implements IRepositorioBase<Asistente, String> {

    // Tabla dependiente: Asistente(CodAsistente FK -> Empleado)
    // En el repositorio devolvemos Asistente con datos de Empleado.

    @Override
    public void insert(Asistente entity) {
        String sql = "INSERT INTO Asistente (CodAsistente) VALUES (?)";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getCodEmpleado());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Asistente: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Asistente> findById(String id) {
        String sql = "SELECT e.CodEmpleado, e.Nombre, e.Numero, e.Direccion, e.CorreoElec, e.AniosExp " +
                "FROM Asistente a INNER JOIN Empleado e ON a.CodAsistente = e.CodEmpleado " +
                "WHERE a.CodAsistente = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Asistente a = new Asistente();
                a.setCodEmpleado(rs.getString("CodEmpleado"));
                a.setNombre(rs.getString("Nombre"));
                a.setNumero(rs.getString("Numero"));
                a.setDireccion(rs.getString("Direccion"));
                a.setCorreoElec(rs.getString("CorreoElec"));
                a.setAniosExp(rs.getShort("AniosExp"));
                return Optional.of(a);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Asistente por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Asistente> findAll() {
        String sql = "SELECT e.CodEmpleado, e.Nombre, e.Numero, e.Direccion, e.CorreoElec, e.AniosExp " +
                "FROM Asistente a INNER JOIN Empleado e ON a.CodAsistente = e.CodEmpleado";
        List<Asistente> result = new ArrayList<>();
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Asistente a = new Asistente();
                a.setCodEmpleado(rs.getString("CodEmpleado"));
                a.setNombre(rs.getString("Nombre"));
                a.setNumero(rs.getString("Numero"));
                a.setDireccion(rs.getString("Direccion"));
                a.setCorreoElec(rs.getString("CorreoElec"));
                a.setAniosExp(rs.getShort("AniosExp"));
                result.add(a);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Asistentes: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Asistente entity) {
        // Como Asistente es tabla dependiente por PK, se actualiza principalmente Empleado.
        // Aquí actualizamos datos en Empleado.
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
            throw new RuntimeException("Error al actualizar Asistente: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        // Primero quitamos el registro dependiente.
        String sql = "DELETE FROM Asistente WHERE CodAsistente = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Asistente: " + e.getMessage(), e);
        }
    }
}

