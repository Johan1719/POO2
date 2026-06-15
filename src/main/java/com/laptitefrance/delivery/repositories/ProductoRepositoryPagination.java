package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.models.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Paginación "real" para Producto (SQL Server con OFFSET/FETCH).
 */
public final class ProductoRepositoryPagination {

    private ProductoRepositoryPagination() {
    }

    public static List<Producto> listarProductosPaginado(String filtro, int page, int pageSize) {
        int p = Math.max(1, page);
        int ps = Math.max(1, pageSize);
        int offset = (p - 1) * ps;

        String f = filtro == null ? "" : filtro.trim();
        boolean filtrar = !f.isEmpty();

        // Orden determinístico para que la paginación sea estable
        // (si agregas más criterios luego, mantener siempre ORDER BY).
        String sql;
        if (filtrar) {
            sql = "SELECT CodProducto, NombreProd, Stock, PrecioProd, CodCat, Activo " +
                  "FROM Producto " +
                  "WHERE CodProducto = ? OR LOWER(NombreProd) LIKE ? " +
                  "ORDER BY NombreProd ASC " +
                  "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        } else {
            sql = "SELECT CodProducto, NombreProd, Stock, PrecioProd, CodCat, Activo " +
                  "FROM Producto " +
                  "ORDER BY NombreProd ASC " +
                  "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        }

        List<Producto> result = new ArrayList<>();

        try (Connection con = DBConnection.getConexion();
             PreparedStatement psStmt = con.prepareStatement(sql)) {

            int idx = 1;
            if (filtrar) {
                psStmt.setString(idx++, f);
                psStmt.setString(idx++, "%" + f.toLowerCase() + "%");
            }

            psStmt.setInt(idx++, offset);
            psStmt.setInt(idx, ps);

            try (ResultSet rs = psStmt.executeQuery()) {
                while (rs.next()) {
                    Producto pr = new Producto();
                    pr.setCodProducto(rs.getString("CodProducto"));
                    pr.setNombreProd(rs.getString("NombreProd"));
                    pr.setStock(rs.getShort("Stock"));
                    pr.setPrecioProd(rs.getDouble("PrecioProd"));
                    pr.setCodCat(rs.getString("CodCat"));
                    pr.setActivo(rs.getBoolean("Activo"));
                    result.add(pr);
                }
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar productos paginado: " + e.getMessage(), e);
        }
    }

    public static int countProductosFiltrados(String filtro) {
        String f = filtro == null ? "" : filtro.trim();
        boolean filtrar = !f.isEmpty();

        String sql;
        if (filtrar) {
            sql = "SELECT COUNT(*) AS total " +
                  "FROM Producto " +
                  "WHERE CodProducto = ? OR LOWER(NombreProd) LIKE ?";
        } else {
            sql = "SELECT COUNT(*) AS total FROM Producto";
        }

        try (Connection con = DBConnection.getConexion();
             PreparedStatement psStmt = con.prepareStatement(sql)) {

            int idx = 1;
            if (filtrar) {
                psStmt.setString(idx++, f);
                psStmt.setString(idx, "%" + f.toLowerCase() + "%");
            }

            try (ResultSet rs = psStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }

            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar productos filtrados: " + e.getMessage(), e);
        }
    }
}

