package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.PedidoProducto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PedidoProductoRepository implements IRepositorioBase<PedidoProducto, String> {

    // ID = codProducto + "|" + codPedido
    private static final String SEP = "|";

    @Override
    public void insert(PedidoProducto entity) {
        String sql = "INSERT INTO Pedido_Producto (CodProducto, CodPedido, CantProd) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getCodProducto());
            ps.setString(2, entity.getCodPedido());
            ps.setShort(3, entity.getCantProd());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Pedido_Producto: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<PedidoProducto> findById(String id) {
        String[] parts = splitId(id);
        String codProducto = parts[0];
        String codPedido = parts[1];

        String sql = "SELECT CodProducto, CodPedido, CantProd FROM Pedido_Producto WHERE CodProducto = ? AND CodPedido = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codProducto);
            ps.setString(2, codPedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                PedidoProducto pp = new PedidoProducto();
                pp.setCodProducto(rs.getString("CodProducto"));
                pp.setCodPedido(rs.getString("CodPedido"));
                pp.setCantProd(rs.getShort("CantProd"));
                return Optional.of(pp);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Pedido_Producto por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PedidoProducto> findAll() {
        String sql = "SELECT CodProducto, CodPedido, CantProd FROM Pedido_Producto";
        List<PedidoProducto> result = new ArrayList<>();
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PedidoProducto pp = new PedidoProducto();
                pp.setCodProducto(rs.getString("CodProducto"));
                pp.setCodPedido(rs.getString("CodPedido"));
                pp.setCantProd(rs.getShort("CantProd"));
                result.add(pp);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Pedido_Producto: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(PedidoProducto entity) {
        String sql = "UPDATE Pedido_Producto SET CantProd = ? WHERE CodProducto = ? AND CodPedido = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setShort(1, entity.getCantProd());
            ps.setString(2, entity.getCodProducto());
            ps.setString(3, entity.getCodPedido());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar Pedido_Producto: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        String[] parts = splitId(id);
        String codProducto = parts[0];
        String codPedido = parts[1];

        String sql = "DELETE FROM Pedido_Producto WHERE CodProducto = ? AND CodPedido = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codProducto);
            ps.setString(2, codPedido);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Pedido_Producto: " + e.getMessage(), e);
        }
    }

    private static String[] splitId(String id) {
        if (id == null || !id.contains(SEP)) {
            throw new IllegalArgumentException("ID inválido para PedidoProducto. Formato esperado: codProducto|codPedido");
        }
        return id.split("\\|", 2);
    }
}

