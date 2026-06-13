package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoRepository implements IRepositorioBase<Producto, String> {

    @Override
    public void insert(Producto entity) {
        String sql = "INSERT INTO Producto (CodProducto, NombreProd, Stock, PrecioProd, CodCat, Activo) VALUES (?, ?, ?, ?, ?, ?)";


        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getCodProducto());
            ps.setString(2, entity.getNombreProd());
            ps.setShort(3, entity.getStock());
            ps.setDouble(4, entity.getPrecioProd());
            ps.setString(5, entity.getCodCat());
            ps.setBoolean(6, entity.isActivo());

            ps.executeUpdate();


        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Producto: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Producto> findById(String id) {
        String sql = "SELECT CodProducto, NombreProd, Stock, PrecioProd, CodCat, Activo FROM Producto WHERE CodProducto = ?";

        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Producto p = new Producto();
                p.setCodProducto(rs.getString("CodProducto"));
                p.setNombreProd(rs.getString("NombreProd"));
                p.setStock(rs.getShort("Stock"));
                p.setPrecioProd(rs.getDouble("PrecioProd"));
                p.setCodCat(rs.getString("CodCat"));
                p.setActivo(rs.getBoolean("Activo"));

                return Optional.of(p);

            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Producto por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Producto> findAll() {
        String sql = "SELECT CodProducto, NombreProd, Stock, PrecioProd, CodCat, Activo FROM Producto";

        List<Producto> result = new ArrayList<>();

        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Producto p = new Producto();
                p.setCodProducto(rs.getString("CodProducto"));
                p.setNombreProd(rs.getString("NombreProd"));
                p.setStock(rs.getShort("Stock"));
                p.setPrecioProd(rs.getDouble("PrecioProd"));
                p.setCodCat(rs.getString("CodCat"));
                p.setActivo(rs.getBoolean("Activo"));
                result.add(p);

            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Productos: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Producto entity) {
        String sql = "UPDATE Producto SET NombreProd = ?, Stock = ?, PrecioProd = ?, CodCat = ?, Activo = ? WHERE CodProducto = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getNombreProd());
            ps.setShort(2, entity.getStock());
            ps.setDouble(3, entity.getPrecioProd());
            ps.setString(4, entity.getCodCat());
            ps.setBoolean(5, entity.isActivo());
            ps.setString(6, entity.getCodProducto());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar Producto: " + e.getMessage(), e);
        }
    }



    @Override
    public void deleteById(String id) {
        // Para evitar conflicto por FK (Pedido_Producto -> Producto)
        // Primero eliminamos filas dependientes.
        String sqlDeletePedidoProducto = "DELETE FROM Pedido_Producto WHERE CodProducto = ?";
        String sqlDeleteProducto = "DELETE FROM Producto WHERE CodProducto = ?";

        try (Connection con = DBConnection.getConexion()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps1 = con.prepareStatement(sqlDeletePedidoProducto);
                 PreparedStatement ps2 = con.prepareStatement(sqlDeleteProducto)) {

                ps1.setString(1, id);
                ps1.executeUpdate();

                ps2.setString(1, id);
                ps2.executeUpdate();

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Producto: " + e.getMessage(), e);
        }
    }

}

