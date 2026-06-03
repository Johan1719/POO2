package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Pago;

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
 * DAO (Repositorio) de Pago — patrón Repository/DAO.
 *
 * Implementa el contrato CRUD de {@link IRepositorioBase} sobre la tabla {@code Pago}
 * de SQL Server, traduciendo entre filas y objetos {@link Pago}. La clave (ID) es
 * {@code CodPago} (String); registra método, monto, IGV, descuentos y costo de tarifa.
 *
 * El detalle de parámetros y retorno de cada método está en {@link IRepositorioBase}.
 * Para fechas usa los helpers {@code setTimestampOrNull} / {@code getTimestampAsLocalDateTime},
 * que convierten de forma segura entre {@link java.time.LocalDateTime} y {@code java.sql.Timestamp}
 * (tolerando valores null). Usa PreparedStatement y try-with-resources.
 */
public class PagoRepository implements IRepositorioBase<Pago, String> {

    @Override
    public void insert(Pago entity) {
        String sql = "INSERT INTO Pago (CodPago, MetodoPago, FechaPago, MontoTotal, Observaciones, CantDesc, IGV, CostoTarifa) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getCodPago());
            ps.setString(2, entity.getMetodoPago());
            setTimestampOrNull(ps, 3, entity.getFechaPago());
            ps.setDouble(4, entity.getMontoTotal());
            ps.setString(5, entity.getObservaciones());
            ps.setDouble(6, entity.getCantDesc());
            ps.setDouble(7, entity.getIgv());
            ps.setDouble(8, entity.getCostoTarifa());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Pago: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Pago> findById(String id) {
        String sql = "SELECT CodPago, MetodoPago, FechaPago, MontoTotal, Observaciones, CantDesc, IGV, CostoTarifa FROM Pago WHERE CodPago = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Pago p = new Pago();
                p.setCodPago(rs.getString("CodPago"));
                p.setMetodoPago(rs.getString("MetodoPago"));
                p.setFechaPago(getTimestampAsLocalDateTime(rs, "FechaPago"));
                p.setMontoTotal(rs.getDouble("MontoTotal"));
                p.setObservaciones(rs.getString("Observaciones"));
                p.setCantDesc(rs.getDouble("CantDesc"));
                p.setIgv(rs.getDouble("IGV"));
                p.setCostoTarifa(rs.getDouble("CostoTarifa"));
                return Optional.of(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Pago por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Pago> findAll() {
        String sql = "SELECT CodPago, MetodoPago, FechaPago, MontoTotal, Observaciones, CantDesc, IGV, CostoTarifa FROM Pago";
        List<Pago> result = new ArrayList<>();
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Pago p = new Pago();
                p.setCodPago(rs.getString("CodPago"));
                p.setMetodoPago(rs.getString("MetodoPago"));
                p.setFechaPago(getTimestampAsLocalDateTime(rs, "FechaPago"));
                p.setMontoTotal(rs.getDouble("MontoTotal"));
                p.setObservaciones(rs.getString("Observaciones"));
                p.setCantDesc(rs.getDouble("CantDesc"));
                p.setIgv(rs.getDouble("IGV"));
                p.setCostoTarifa(rs.getDouble("CostoTarifa"));
                result.add(p);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Pagos: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Pago entity) {
        String sql = "UPDATE Pago SET MetodoPago = ?, FechaPago = ?, MontoTotal = ?, Observaciones = ?, CantDesc = ?, IGV = ?, CostoTarifa = ? WHERE CodPago = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getMetodoPago());
            setTimestampOrNull(ps, 2, entity.getFechaPago());
            ps.setDouble(3, entity.getMontoTotal());
            ps.setString(4, entity.getObservaciones());
            ps.setDouble(5, entity.getCantDesc());
            ps.setDouble(6, entity.getIgv());
            ps.setDouble(7, entity.getCostoTarifa());
            ps.setString(8, entity.getCodPago());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar Pago: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM Pago WHERE CodPago = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Pago: " + e.getMessage(), e);
        }
    }

    /**
     * Fija un parámetro de fecha en el PreparedStatement, tolerando null.
     * @param ps    sentencia preparada destino.
     * @param index posición (1-based) del parámetro {@code ?}.
     * @param value fecha a fijar (LocalDateTime); si es null, se inserta NULL en la BD.
     */
    private static void setTimestampOrNull(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) ps.setTimestamp(index, null);
        else ps.setTimestamp(index, Timestamp.valueOf(value));
    }

    /**
     * Lee una columna de fecha del ResultSet y la convierte a LocalDateTime.
     * @param rs     resultado de la consulta.
     * @param column nombre de la columna de tipo fecha/hora.
     * @return el {@link LocalDateTime} leído, o null si la columna era NULL.
     */
    private static LocalDateTime getTimestampAsLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }
}

