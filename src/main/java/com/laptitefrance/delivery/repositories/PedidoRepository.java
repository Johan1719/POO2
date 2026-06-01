package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Pedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PedidoRepository implements IRepositorioBase<Pedido, String> {

    @Override
    public void insert(Pedido entity) {
        // Asumimos columnas según modelo (y script validado).
        String sql = "INSERT INTO Pedido (CodPedido, FechaSolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, CodRepartidor, IDCliente, CodTarifa, CodPago) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, entity.getCodPedido());
            setTimestampOrNull(ps, 2, entity.getFechaSolicitud());
            ps.setDouble(3, entity.getMontoPedido());
            ps.setString(4, entity.getEstado());
            setTimestampOrNull(ps, 5, entity.getTiempoEntEstimado());
            setTimestampOrNull(ps, 6, entity.getTiempoEntReal());
            ps.setString(7, entity.getCodRepartidor());
            ps.setString(8, entity.getIdCliente());
            ps.setString(9, entity.getCodTarifa());
            ps.setString(10, entity.getCodPago());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Pedido> findById(String id) {
        String sql = "SELECT CodPedido, FechaSolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, CodRepartidor, IDCliente, CodTarifa, CodPago " +
                "FROM Pedido WHERE CodPedido = ?";

        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                Pedido p = new Pedido();
                p.setCodPedido(rs.getString("CodPedido"));
                p.setFechaSolicitud(getTimestampAsLocalDateTime(rs, "FechaSolicitud"));
                p.setMontoPedido(rs.getDouble("MontoPedido"));
                p.setEstado(rs.getString("Estado"));
                p.setTiempoEntEstimado(getTimestampAsLocalDateTime(rs, "TiempoEntEstimado"));
                p.setTiempoEntReal(getTimestampAsLocalDateTime(rs, "TiempoEntReal"));
                p.setCodRepartidor(rs.getString("CodRepartidor"));
                p.setIdCliente(rs.getString("IDCliente"));
                p.setCodTarifa(rs.getString("CodTarifa"));
                p.setCodPago(rs.getString("CodPago"));
                return Optional.of(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Pedido por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Pedido> findAll() {
        String sql = "SELECT CodPedido, FechaSolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, CodRepartidor, IDCliente, CodTarifa, CodPago FROM Pedido";
        List<Pedido> result = new ArrayList<>();

        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pedido p = new Pedido();
                p.setCodPedido(rs.getString("CodPedido"));
                p.setFechaSolicitud(getTimestampAsLocalDateTime(rs, "FechaSolicitud"));
                p.setMontoPedido(rs.getDouble("MontoPedido"));
                p.setEstado(rs.getString("Estado"));
                p.setTiempoEntEstimado(getTimestampAsLocalDateTime(rs, "TiempoEntEstimado"));
                p.setTiempoEntReal(getTimestampAsLocalDateTime(rs, "TiempoEntReal"));
                p.setCodRepartidor(rs.getString("CodRepartidor"));
                p.setIdCliente(rs.getString("IDCliente"));
                p.setCodTarifa(rs.getString("CodTarifa"));
                p.setCodPago(rs.getString("CodPago"));

                result.add(p);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Pedidos: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Pedido entity) {
        String sql = "UPDATE Pedido SET FechaSolicitud = ?, MontoPedido = ?, Estado = ?, TiempoEntEstimado = ?, TiempoEntReal = ?, CodRepartidor = ?, IDCliente = ?, CodTarifa = ?, CodPago = ? " +
                "WHERE CodPedido = ?";

        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setTimestampOrNull(ps, 1, entity.getFechaSolicitud());
            ps.setDouble(2, entity.getMontoPedido());
            ps.setString(3, entity.getEstado());
            setTimestampOrNull(ps, 4, entity.getTiempoEntEstimado());
            setTimestampOrNull(ps, 5, entity.getTiempoEntReal());
            ps.setString(6, entity.getCodRepartidor());
            ps.setString(7, entity.getIdCliente());
            ps.setString(8, entity.getCodTarifa());
            ps.setString(9, entity.getCodPago());
            ps.setString(10, entity.getCodPedido());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar Pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM Pedido WHERE CodPedido = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Pedido: " + e.getMessage(), e);
        }
    }

    private static void setTimestampOrNull(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setTimestamp(index, null);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private static LocalDateTime getTimestampAsLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }
}

