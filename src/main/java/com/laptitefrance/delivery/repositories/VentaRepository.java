package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.dtos.ItemVenta;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Pedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones de venta que tocan varias tablas (Pedido, Pedido_Producto, Producto)
 * dentro de una sola transacción.
 */
public class VentaRepository {

    private static final String INSERT_PEDIDO =
            "INSERT INTO Pedido (FechaSolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, HoraEnvio, DireccionEntrega, CodAsistente, CodRepartidor, IDCliente, CodTarifa, CodPago) " +
            "OUTPUT INSERTED.CodPedido " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_PRODUCTO = "SELECT NombreProd, Stock FROM Producto WHERE CodProducto = ?";
    private static final String INSERT_PP = "INSERT INTO Pedido_Producto (CodProducto, CodPedido, CantProd) VALUES (?, ?, ?)";
    private static final String UPDATE_STOCK = "UPDATE Producto SET Stock = ? WHERE CodProducto = ?";

    /**
     * Inserta el pedido y sus ítems, descontando stock, todo en una transacción.
     * @return nombres de productos cuyo stock quedó en 0.
     * @throws ValidationException si algún ítem no tiene stock suficiente.
     */
    public List<String> registrarVenta(Pedido pedido, List<ItemVenta> items) {
        List<String> productosEnCero = new ArrayList<>();

        try (Connection con = DBConnection.getConexion()) {
            con.setAutoCommit(false);
            try {
                // 1) Insertar Pedido y recuperar el CodPedido autogenerado.
                String codPedido;
                try (PreparedStatement ps = con.prepareStatement(INSERT_PEDIDO)) {
                    setTimestampOrNull(ps, 1, pedido.getFechaSolicitud());
                    ps.setDouble(2, pedido.getMontoPedido());
                    ps.setString(3, pedido.getEstado());
                    setTimestampOrNull(ps, 4, pedido.getTiempoEntEstimado());
                    setTimestampOrNull(ps, 5, pedido.getTiempoEntReal());
                    setTimestampOrNull(ps, 6, pedido.getHoraEnvio());
                    ps.setString(7, pedido.getDireccionEntrega());
                    ps.setString(8, pedido.getCodAsistente());
                    ps.setString(9, pedido.getCodRepartidor());
                    ps.setString(10, pedido.getIdCliente());
                    ps.setString(11, pedido.getCodTarifa());
                    ps.setString(12, pedido.getCodPago());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("No se pudo obtener el CodPedido generado.");
                        }
                        codPedido = rs.getString(1);
                    }
                }

                // 2) Por cada ítem: validar stock, insertar Pedido_Producto, descontar.
                for (ItemVenta item : items) {
                    String nombre;
                    int stockActual;
                    try (PreparedStatement ps = con.prepareStatement(SELECT_PRODUCTO)) {
                        ps.setString(1, item.getCodProducto());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new ValidationException("No existe el producto con código: " + item.getCodProducto());
                            }
                            nombre = rs.getString("NombreProd");
                            stockActual = rs.getShort("Stock");
                        }
                    }

                    if (stockActual < item.getCantidad()) {
                        throw new ValidationException(
                                "Stock insuficiente de " + nombre + ": hay " + stockActual + ", pediste " + item.getCantidad());
                    }

                    try (PreparedStatement ps = con.prepareStatement(INSERT_PP)) {
                        ps.setString(1, item.getCodProducto());
                        ps.setString(2, codPedido);
                        ps.setShort(3, (short) item.getCantidad());
                        ps.executeUpdate();
                    }

                    int nuevoStock = stockActual - item.getCantidad();
                    try (PreparedStatement ps = con.prepareStatement(UPDATE_STOCK)) {
                        ps.setShort(1, (short) nuevoStock);
                        ps.setString(2, item.getCodProducto());
                        ps.executeUpdate();
                    }

                    if (nuevoStock == 0) {
                        productosEnCero.add(nombre);
                    }
                }

                con.commit();
                return productosEnCero;
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar la venta: " + e.getMessage(), e);
        }
    }

    private static void setTimestampOrNull(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setTimestamp(index, null);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
    }
}
