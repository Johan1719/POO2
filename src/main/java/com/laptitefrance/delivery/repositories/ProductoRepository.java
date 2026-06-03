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

/**
 * DAO (Repositorio) de Producto — patrón Repository/DAO.
 *
 * Implementa el contrato CRUD de {@link IRepositorioBase} sobre la tabla {@code Producto}
 * de SQL Server, traduciendo entre filas y objetos {@link Producto}. La clave (ID) es
 * {@code CodProducto} (String); incluye stock, precio y la categoría ({@code CodCat}).
 * Lo usa {@code ProductoController} para mostrar el menú disponible.
 *
 * El detalle de parámetros y retorno de cada método está en {@link IRepositorioBase}.
 * Usa PreparedStatement y try-with-resources; los errores SQL se envuelven en RuntimeException.
 */
public class ProductoRepository implements IRepositorioBase<Producto, String> {

    @Override
    public void insert(Producto entity) {
        String sql = "INSERT INTO Producto (CodProducto, NombreProd, Stock, PrecioProd, CodCat) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getCodProducto());
            ps.setString(2, entity.getNombreProd());
            ps.setShort(3, entity.getStock());
            ps.setDouble(4, entity.getPrecioProd());
            ps.setString(5, entity.getCodCat());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Producto: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Producto> findById(String id) {
        String sql = "SELECT CodProducto, NombreProd, Stock, PrecioProd, CodCat FROM Producto WHERE CodProducto = ?";
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
                return Optional.of(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Producto por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Producto> findAll() {
        String sql = "SELECT CodProducto, NombreProd, Stock, PrecioProd, CodCat FROM Producto";
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
                result.add(p);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Productos: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Producto entity) {
        String sql = "UPDATE Producto SET NombreProd = ?, Stock = ?, PrecioProd = ?, CodCat = ? WHERE CodProducto = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getNombreProd());
            ps.setShort(2, entity.getStock());
            ps.setDouble(3, entity.getPrecioProd());
            ps.setString(4, entity.getCodCat());
            ps.setString(5, entity.getCodProducto());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar Producto: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM Producto WHERE CodProducto = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Producto: " + e.getMessage(), e);
        }
    }
}

