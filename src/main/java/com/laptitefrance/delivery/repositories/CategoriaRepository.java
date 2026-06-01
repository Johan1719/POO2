package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoriaRepository implements IRepositorioBase<Categoria, String> {

    @Override
    public void insert(Categoria entity) {
        String sql = "INSERT INTO Categoria (CodCat, NombreCat) VALUES (?, ?)";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getCodCat());
            ps.setString(2, entity.getNombreCat());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar Categoria: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Categoria> findById(String id) {
        String sql = "SELECT CodCat, NombreCat FROM Categoria WHERE CodCat = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Categoria c = new Categoria();
                c.setCodCat(rs.getString("CodCat"));
                c.setNombreCat(rs.getString("NombreCat"));
                return Optional.of(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar Categoria por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Categoria> findAll() {
        String sql = "SELECT CodCat, NombreCat FROM Categoria";
        List<Categoria> result = new ArrayList<>();
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Categoria c = new Categoria();
                c.setCodCat(rs.getString("CodCat"));
                c.setNombreCat(rs.getString("NombreCat"));
                result.add(c);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar Categorias: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Categoria entity) {
        String sql = "UPDATE Categoria SET NombreCat = ? WHERE CodCat = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, entity.getNombreCat());
            ps.setString(2, entity.getCodCat());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar Categoria: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM Categoria WHERE CodCat = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar Categoria: " + e.getMessage(), e);
        }
    }
}

