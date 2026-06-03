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
 * DAO (Repositorio) de Cliente — patrón Repository/DAO.
 *
 * Implementa el contrato CRUD de {@link IRepositorioBase} sobre la tabla {@code Cliente}
 * de SQL Server, traduciendo entre filas y objetos {@link Cliente}. La clave (ID) es
 * {@code IDCliente} (String). Al insertar, la fecha de registro la pone la BD con {@code GETDATE()}.
 *
 * Además del CRUD estándar, expone el método extra {@link #findByTelefono(String)}, usado por
 * {@code ClienteController} para identificar al cliente por su número de celular en la venta.
 *
 * El detalle de parámetros y retorno de los métodos del contrato está en {@link IRepositorioBase}.
 */
public class ClienteRepository implements IRepositorioBase<Cliente, String> {

    @Override
    public void insert(Cliente entity) {
        String sql = "INSERT INTO Cliente (IDCliente, FechaRegistro, NombreCliente, Nrocelular) VALUES (?, GETDATE(), ?, ?)";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, entity.getIdCliente());
            ps.setString(2, entity.getNombreCliente());
            ps.setString(3, entity.getNrocelular());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Cliente> findById(String id) {
        String sql = "SELECT IDCliente, FechaRegistro, NombreCliente, Nrocelular FROM Cliente WHERE IDCliente = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                Cliente c = new Cliente();
                c.setIdCliente(rs.getString("IDCliente"));
                Timestamp ts = rs.getTimestamp("FechaRegistro");
                c.setFechaRegistro(ts != null ? ts.toLocalDateTime() : null);
                c.setNombreCliente(rs.getString("NombreCliente"));
                c.setNrocelular(rs.getString("Nrocelular"));
                return Optional.of(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Cliente por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Cliente> findAll() {
        String sql = "SELECT IDCliente, FechaRegistro, NombreCliente, Nrocelular FROM Cliente";
        List<Cliente> result = new ArrayList<>();
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Cliente c = new Cliente();
                c.setIdCliente(rs.getString("IDCliente"));
                Timestamp ts = rs.getTimestamp("FechaRegistro");
                c.setFechaRegistro(ts != null ? ts.toLocalDateTime() : null);
                c.setNombreCliente(rs.getString("NombreCliente"));
                c.setNrocelular(rs.getString("Nrocelular"));
                result.add(c);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Clientes: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Cliente entity) {
        String sql = "UPDATE Cliente SET FechaRegistro = ?, NombreCliente = ?, Nrocelular = ? WHERE IDCliente = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            LocalDateTime fecha = entity.getFechaRegistro();
            ps.setTimestamp(1, fecha != null ? Timestamp.valueOf(fecha) : null);
            ps.setString(2, entity.getNombreCliente());
            ps.setString(3, entity.getNrocelular());
            ps.setString(4, entity.getIdCliente());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar Cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM Cliente WHERE IDCliente = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Cliente: " + e.getMessage(), e);
        }
    }

    /**
     * Método extra (fuera del contrato CRUD): busca un cliente por su número de celular.
     * Lo usa la vista del Asistente / {@code ClienteController} para identificar al cliente.
     *
     * @param celular número de celular (String) a buscar (coincidencia exacta).
     * @return Optional&lt;Cliente&gt; con el cliente si existe, o Optional.empty() si ninguno coincide.
     */
    public Optional<Cliente> findByTelefono(String celular) {
        String sql = "SELECT IDCliente, FechaRegistro, NombreCliente, Nrocelular FROM Cliente WHERE Nrocelular = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, celular);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                Cliente c = new Cliente();
                c.setIdCliente(rs.getString("IDCliente"));
                Timestamp ts = rs.getTimestamp("FechaRegistro");
                c.setFechaRegistro(ts != null ? ts.toLocalDateTime() : null);
                c.setNombreCliente(rs.getString("NombreCliente"));
                c.setNrocelular(rs.getString("Nrocelular"));
                return Optional.of(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Cliente por Celular: " + e.getMessage(), e);
        }
    }
}