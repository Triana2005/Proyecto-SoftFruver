package com.softfruver.inventario.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
public class ProductoReadController {

    private final JdbcTemplate jdbc;

    public ProductoReadController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // GET /api/productos?limit=50
    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(defaultValue = "50") int limit) {
        if (limit < 1 || limit > 500)
            limit = 50;

        // Usa parámetro para LIMIT (PostgreSQL lo soporta)
        String sql = """
                SELECT id, nombre, precio_venta_actual, activo, creado_en
                FROM producto
                ORDER BY id
                LIMIT ?
                """;

        return jdbc.queryForList(sql, limit);
    }

    // GET /api/productos/{id}
    @GetMapping("/{id}")
    public Map<String, Object> porId(@PathVariable Integer id) {
        String sql = """
                SELECT id, nombre, precio_venta_actual, activo, creado_en
                FROM producto
                WHERE id = ?
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, id);
        if (rows.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Producto no encontrado");
        }
        return rows.get(0);
    }
}
