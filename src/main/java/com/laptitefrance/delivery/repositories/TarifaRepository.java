package com.laptitefrance.delivery.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Tarifa;

/**
 * DAO (Repositorio) de Tarifa — patrón Repository/DAO.
 *
 * Implementa el contrato CRUD de {@link IRepositorioBase} sobre la tabla {@code Tarifa}
 * de SQL Server, traduciendo entre filas y objetos {@link Tarifa}. La clave (ID) es
 * {@code CodTarifa} (String); cada tarifa lleva zona, precio y {@code TiempoPromedio} (int, minutos).
 * Lo usa {@code TarifaController} para ofrecer las opciones de envío en la venta.
 *
 * El detalle de parámetros y retorno de cada método está en {@link IRepositorioBase}.
 * Usa PreparedStatement y try-with-resources; los errores SQL se envuelven en RuntimeException.
 */
public class TarifaRepository implements IRepositorioBase<Tarifa, String> {

    @Override
    public void insert(Tarifa entity) {
        String sql = "INSERT INTO Tarifa (CodTarifa, NombreZona, PrecioTarifa, TiempoPromedio) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getCodTarifa());
            ps.setString(2, entity.getNombreZona());
            ps.setDouble(3, entity.getPrecioTarifa());
            ps.setInt(4, entity.getTiempoPromedio());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Tarifa: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Tarifa> findById(String id) {
        String sql = "SELECT CodTarifa, NombreZona, PrecioTarifa, TiempoPromedio FROM Tarifa WHERE CodTarifa = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Tarifa t = new Tarifa();
                t.setCodTarifa(rs.getString("CodTarifa"));
                t.setNombreZona(rs.getString("NombreZona"));
                t.setPrecioTarifa(rs.getDouble("PrecioTarifa"));
                t.setTiempoPromedio(rs.getInt("TiempoPromedio"));
                return Optional.of(t);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Tarifa por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Tarifa> findAll() {
        String sql = "SELECT CodTarifa, NombreZona, PrecioTarifa, TiempoPromedio FROM Tarifa";
        List<Tarifa> result = new ArrayList<>();
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Tarifa t = new Tarifa();
                t.setCodTarifa(rs.getString("CodTarifa"));
                t.setNombreZona(rs.getString("NombreZona"));
                t.setPrecioTarifa(rs.getDouble("PrecioTarifa"));
                t.setTiempoPromedio(rs.getInt("TiempoPromedio"));
                result.add(t);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Tarifas: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Tarifa entity) {
        String sql = "UPDATE Tarifa SET NombreZona = ?, PrecioTarifa = ?, TiempoPromedio = ? WHERE CodTarifa = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getNombreZona());
            ps.setDouble(2, entity.getPrecioTarifa());
            ps.setInt(3, entity.getTiempoPromedio());
            ps.setString(4, entity.getCodTarifa());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar Tarifa: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM Tarifa WHERE CodTarifa = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Tarifa: " + e.getMessage(), e);
        }
    }
}
