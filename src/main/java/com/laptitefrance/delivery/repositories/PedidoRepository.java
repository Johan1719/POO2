package com.laptitefrance.delivery.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Pedido;

/**
 * DAO (Repositorio) de Pedido — patrón Repository/DAO. Es el repositorio central del sistema.
 *
 * Implementa el contrato CRUD de {@link IRepositorioBase} sobre la tabla {@code Pedido}
 * de SQL Server, traduciendo entre filas y objetos {@link Pedido}. La clave (ID) es
 * {@code CodPedido} (String). Maneja varias columnas de fecha/hora (FechaSolicitud,
 * TiempoEntEstimado, TiempoEntReal, HoraEnvio) y las claves foráneas a Cliente, Asistente,
 * Repartidor, Tarifa y Pago.
 *
 * Lo usa {@code PedidoController} para crear pedidos, listarlos, asignar repartidor y cambiar estado.
 * El detalle de parámetros y retorno de cada método está en {@link IRepositorioBase}.
 * Las fechas se convierten con los helpers {@code setTimestampOrNull} / {@code getTimestampAsLocalDateTime},
 * que toleran valores null. Usa PreparedStatement (anti-inyección SQL) y try-with-resources.
 */
public class PedidoRepository implements IRepositorioBase<Pedido, String> {

    @Override
    public void insert(Pedido entity) {
        // Asumimos columnas según modelo (y script validado).
        String sql = "INSERT INTO Pedido (CodPedido, FechaSolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, HoraEnvio, DireccionEntrega, CodAsistente, CodRepartidor, IDCliente, CodTarifa, CodPago) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";



        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, entity.getCodPedido());
            setTimestampOrNull(ps, 2, entity.getFechaSolicitud());
            ps.setDouble(3, entity.getMontoPedido());
            ps.setString(4, entity.getEstado());
            setTimestampOrNull(ps, 5, entity.getTiempoEntEstimado());
            setTimestampOrNull(ps, 6, entity.getTiempoEntReal());

            // 7 = HoraEnvio
            setTimestampOrNull(ps, 7, entity.getHoraEnvio());

            // 8..13 restantes
            ps.setString(8, entity.getDireccionEntrega());
            ps.setString(9, entity.getCodAsistente());
            ps.setString(10, entity.getCodRepartidor());
            ps.setString(11, entity.getIdCliente());
            ps.setString(12, entity.getCodTarifa());
            ps.setString(13, entity.getCodPago());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Pedido: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Pedido> findById(String id) {
        String sql = "SELECT CodPedido, FechaSolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, HoraEnvio, DireccionEntrega, CodAsistente, CodRepartidor, IDCliente, CodTarifa, CodPago " +
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
                p.setHoraEnvio(getTimestampAsLocalDateTime(rs, "HoraEnvio"));
                p.setDireccionEntrega(rs.getString("DireccionEntrega"));

                p.setCodAsistente(rs.getString("CodAsistente"));
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
        String sql = "SELECT CodPedido, FechaSolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, HoraEnvio, DireccionEntrega, CodAsistente, CodRepartidor, IDCliente, CodTarifa, CodPago FROM Pedido";


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
                p.setHoraEnvio(getTimestampAsLocalDateTime(rs, "HoraEnvio"));
                p.setDireccionEntrega(rs.getString("DireccionEntrega"));

                p.setCodAsistente(rs.getString("CodAsistente"));
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
        String sql = "UPDATE Pedido SET FechaSolicitud = ?, MontoPedido = ?, Estado = ?, TiempoEntEstimado = ?, TiempoEntReal = ?, HoraEnvio = ?, DireccionEntrega = ?, CodAsistente = ?, CodRepartidor = ?, IDCliente = ?, CodTarifa = ?, CodPago = ? " +
                "WHERE CodPedido = ?";



        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setTimestampOrNull(ps, 1, entity.getFechaSolicitud());
            ps.setDouble(2, entity.getMontoPedido());
            ps.setString(3, entity.getEstado());
            setTimestampOrNull(ps, 4, entity.getTiempoEntEstimado());
            setTimestampOrNull(ps, 5, entity.getTiempoEntReal());
            setTimestampOrNull(ps, 6, entity.getHoraEnvio());
            ps.setString(7, entity.getDireccionEntrega());
            ps.setString(8, entity.getCodAsistente());
            ps.setString(9, entity.getCodRepartidor());

            ps.setString(10, entity.getIdCliente());

            ps.setString(11, entity.getCodTarifa());
            ps.setString(12, entity.getCodPago());
            ps.setString(13, entity.getCodPedido());



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

    /**
     * Fija un parámetro de fecha en el PreparedStatement, tolerando null.
     * @param ps    sentencia preparada destino.
     * @param index posición (1-based) del parámetro {@code ?}.
     * @param value fecha a fijar (LocalDateTime); si es null, se inserta NULL en la BD.
     */
    private static void setTimestampOrNull(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setTimestamp(index, null);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
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

