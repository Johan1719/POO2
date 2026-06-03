package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Empleado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO (Repositorio) de Empleado — patrón Repository/DAO.
 *
 * Implementa el contrato CRUD de {@link IRepositorioBase} sobre la tabla {@code Empleado}
 * de SQL Server, traduciendo entre filas de la BD y objetos {@link Empleado}. La clave (ID)
 * es {@code CodEmpleado} (String). Lo usa, por ejemplo, {@code LoginController} para autenticar.
 *
 * El detalle de parámetros y valores de retorno de cada método está en {@link IRepositorioBase}.
 * Todas las consultas usan {@code PreparedStatement} (previene inyección SQL) y conexiones
 * con try-with-resources (se cierran solas). Los errores SQL se envuelven en RuntimeException.
 */
public class EmpleadoRepository implements IRepositorioBase<Empleado, String> {

    @Override
    public void insert(Empleado entity) {
        String sql = "INSERT INTO Empleado (CodEmpleado, Nombre, Numero, Direccion, CorreoElec, AniosExp) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getCodEmpleado());
            ps.setString(2, entity.getNombre());
            ps.setString(3, entity.getNumero());
            ps.setString(4, entity.getDireccion());
            ps.setString(5, entity.getCorreoElec());
            ps.setShort(6, entity.getAniosExp());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Empleado: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Empleado> findById(String id) {
        String sql = "SELECT CodEmpleado, Nombre, Numero, Direccion, CorreoElec, AniosExp FROM Empleado WHERE CodEmpleado = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Empleado e = new Empleado();
                e.setCodEmpleado(rs.getString("CodEmpleado"));
                e.setNombre(rs.getString("Nombre"));
                e.setNumero(rs.getString("Numero"));
                e.setDireccion(rs.getString("Direccion"));
                e.setCorreoElec(rs.getString("CorreoElec"));
                e.setAniosExp(rs.getShort("AniosExp"));
                return Optional.of(e);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error al consultar Empleado por ID: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Empleado> findAll() {
        String sql = "SELECT CodEmpleado, Nombre, Numero, Direccion, CorreoElec, AniosExp FROM Empleado";
        List<Empleado> result = new ArrayList<>();
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Empleado e = new Empleado();
                e.setCodEmpleado(rs.getString("CodEmpleado"));
                e.setNombre(rs.getString("Nombre"));
                e.setNumero(rs.getString("Numero"));
                e.setDireccion(rs.getString("Direccion"));
                e.setCorreoElec(rs.getString("CorreoElec"));
                e.setAniosExp(rs.getShort("AniosExp"));
                result.add(e);
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException("Error al listar Empleados: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void update(Empleado entity) {
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
        } catch (SQLException ex) {
            throw new RuntimeException("Error al actualizar Empleado: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM Empleado WHERE CodEmpleado = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Error al eliminar Empleado: " + ex.getMessage(), ex);
        }
    }
}

