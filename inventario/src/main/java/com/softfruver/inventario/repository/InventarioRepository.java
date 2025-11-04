package com.softfruver.inventario.repository;

import com.softfruver.inventario.repository.projection.InventarioItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InventarioRepository {

    private final JdbcTemplate jdbc;
    // El umbral lo usamos solo si en algún momento filtras en SQL directo;
    // hoy el conteo de alertas usa la vista v_stock_alerta.
    @SuppressWarnings("unused")
    private static final double UMBRAL_ALERTA_KG = 20.0;

    public InventarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Lista TODOS los productos con su stock (usa la vista creada en V8) */
    public List<InventarioItem> listar() {
        final String sql = """
            SELECT nombre, stock_kg
            FROM softfruver.v_inventario_listado
            ORDER BY nombre
        """;
        return jdbc.query(sql, (rs, i) ->
            new InventarioItem(
                rs.getString("nombre"),
                rs.getDouble("stock_kg")
            )
        );
    }

    /** Búsqueda por nombre (case-insensitive, con ILIKE) */
    public List<InventarioItem> buscar(String q) {
        final String sql = """
            SELECT nombre, stock_kg
            FROM softfruver.v_inventario_listado
            WHERE nombre ILIKE ?
            ORDER BY nombre
        """;
        return jdbc.query(sql, ps -> ps.setString(1, "%" + q + "%"),
            (rs, i) -> new InventarioItem(
                rs.getString("nombre"),
                rs.getDouble("stock_kg")
            )
        );
    }

    /** Total de productos del catálogo (tabla real) */
    public long totalProductos() {
        final String sql = "SELECT COUNT(*) FROM softfruver.producto";
        final Long n = jdbc.queryForObject(sql, Long.class);
        return n != null ? n : 0L;
    }

    /** Total de alertas (usa la vista ya calculada por tu dump) */
    public long totalAlertas() {
        final String sql = "SELECT COUNT(*) FROM softfruver.v_stock_alerta";
        final Long n = jdbc.queryForObject(sql, Long.class);
        return n != null ? n : 0L;
    }
}
