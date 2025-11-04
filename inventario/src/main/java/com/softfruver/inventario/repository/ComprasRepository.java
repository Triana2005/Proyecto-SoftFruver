package com.softfruver.inventario.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ComprasRepository {

    private final NamedParameterJdbcTemplate jdbcNamed;
    private final JdbcTemplate jdbc;

    public ComprasRepository(NamedParameterJdbcTemplate jdbcNamed, JdbcTemplate jdbc) {
        this.jdbcNamed = jdbcNamed;
        this.jdbc = jdbc;
    }

    // === Proyecciones locales (records) ===
    public record CompraItem(
            Long id,                // <<-- NECESARIO para acciones en lista
            LocalDate fecha,
            String proveedor,
            String productos,
            BigDecimal total) {}

    public record CompraDetalleItem(
            Long productoId,
            String producto,
            BigDecimal cantidadKg,
            BigDecimal precioUnitario) {}

    public record Opcion(Long id, String nombre) {}

    // Exponemos proveedorId para el form-editar
    public record CompraCabecera(
            Long id,
            LocalDate fecha,
            Long proveedorId,
            String proveedor,
            BigDecimal total) {}

    public record NuevaCompraItem(Long productoId, BigDecimal cantidadKg, BigDecimal precioUnit) {}

    // ====== LISTA (con resumen de productos) ======
    public List<CompraItem> buscarCompras(LocalDate desde, LocalDate hasta, String proveedorNombre) {
        String sql = """
                select
                  c.id                                   as id,
                  cast(c.fecha_compra as date)           as fecha,
                  p.nombre                               as proveedor,
                  coalesce(
                    string_agg(
                      (p2.nombre || ' × ' || trim(to_char(ci.cantidad_kg, 'FM999999990.##')) || ' kg'),
                      ' · '
                      order by p2.nombre
                    ),
                    '—'
                  )                                       as productos,
                  c.total                                as total
                from softfruver.compra c
                join softfruver.proveedor p on p.id = c.proveedor_id
                left join softfruver.compra_item ci on ci.compra_id = c.id
                left join softfruver.producto    p2 on p2.id = ci.producto_id
                where
                  c.fecha_compra >= coalesce(cast(:desde as timestamp), '-infinity'::timestamp)
                  and c.fecha_compra <  coalesce(cast(:hasta as timestamp) + interval '1 day', 'infinity'::timestamp)
                  and (
                    coalesce(:prov, '') = ''
                    or p.nombre ilike concat('%', :prov, '%')
                  )
                group by c.id, cast(c.fecha_compra as date), p.nombre, c.total
                order by cast(c.fecha_compra as date) desc, c.id desc
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("desde", desde)
                .addValue("hasta", hasta)
                .addValue("prov", (proveedorNombre == null || proveedorNombre.isBlank()) ? "" : proveedorNombre.trim());

        return jdbcNamed.query(sql, params, (ResultSet rs, int rowNum) -> new CompraItem(
                rs.getLong("id"),
                rs.getDate("fecha").toLocalDate(),
                rs.getString("proveedor"),
                rs.getString("productos"),
                rs.getBigDecimal("total")));
    }

    // ====== CABECERA / DETALLE ======
    public CompraCabecera obtenerCabecera(Long compraId) {
        String sql = """
                select
                  c.id                                   as id,
                  cast(c.fecha_compra as date)           as fecha,
                  c.proveedor_id                         as proveedor_id,
                  p.nombre                               as proveedor,
                  c.total                                as total
                from softfruver.compra c
                join softfruver.proveedor p on p.id = c.proveedor_id
                where c.id = :id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", compraId);

        return jdbcNamed.query(sql, params, (rs) -> {
            if (rs.next()) {
                return new CompraCabecera(
                        rs.getLong("id"),
                        rs.getDate("fecha").toLocalDate(),
                        rs.getLong("proveedor_id"),
                        rs.getString("proveedor"),
                        rs.getBigDecimal("total"));
            }
            return null;
        });
    }

    public List<CompraDetalleItem> obtenerDetalle(Long compraId) {
        String sql = """
                select
                  ci.producto_id   as "productoId",
                  p.nombre         as "producto",
                  ci.cantidad_kg   as "cantidadKg",
                  ci.precio_unit   as "precioUnitario"
                from softfruver.compra_item ci
                join softfruver.producto p on p.id = ci.producto_id
                where ci.compra_id = :compraId
                order by p.nombre
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("compraId", compraId);

        return jdbcNamed.query(sql, params, (rs, i) -> new CompraDetalleItem(
                rs.getLong("productoId"),
                rs.getString("producto"),
                rs.getBigDecimal("cantidadKg"),
                rs.getBigDecimal("precioUnitario")));
    }

    // ====== OPCIONES ======
    public List<Opcion> opcionesProveedores() {
        String sql = """
                select id, nombre
                from softfruver.proveedor
                order by nombre
                """;
        return jdbcNamed.query(sql, (rs, i) -> new Opcion(rs.getLong("id"), rs.getString("nombre")));
    }

    public List<Opcion> opcionesProductos() {
        String sql = """
                select id, nombre
                from softfruver.producto
                order by nombre
                """;
        return jdbcNamed.query(sql, (rs, i) -> new Opcion(rs.getLong("id"), rs.getString("nombre")));
    }

    // ====== INSERT CABECERA (INSERT + currval + UPDATE fecha) ======
    public Long insertCabecera(Long proveedorId, LocalDate fecha) {
        final String insertSql = "insert into softfruver.compra (proveedor_id, total) values (?, ?)";

        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(insertSql);
                ps.setLong(1, proveedorId);
                ps.setBigDecimal(2, BigDecimal.ZERO);
                return ps;
            });

            Long id = jdbc.queryForObject(
                    "select currval(pg_get_serial_sequence('softfruver.compra','id'))",
                    Long.class);
            if (id == null)
                throw new IllegalStateException("No se obtuvo ID generado (currval() = null).");

            final String updateSql = "update softfruver.compra set fecha_compra = ? where id = ?";
            LocalDateTime ldt = fecha.atStartOfDay();
            Timestamp ts = Timestamp.valueOf(ldt);

            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(updateSql);
                ps.setTimestamp(1, ts);
                ps.setLong(2, id);
                return ps;
            });

            return id;

        } catch (Exception ex) {
            String msg = "Error al insertar/actualizar cabecera de compra. proveedor_id=" +
                    proveedorId + ", fecha=" + fecha + ". Causa: " + ex.getClass().getName() + " - " + ex.getMessage();
            throw new RuntimeException(msg, ex);
        }
    }

    // ====== UPDATE CABECERA ======
    public void updateCabecera(Long compraId, Long proveedorId, LocalDate fecha) {
        final String sql = """
                update softfruver.compra
                   set proveedor_id = ?,
                       fecha_compra = ?
                 where id = ?
                """;
        Timestamp ts = Timestamp.valueOf(fecha.atStartOfDay());
        jdbc.update(sql, proveedorId, ts, compraId);
    }

    // ====== INSERT ITEMS ======
    public void insertItems(Long compraId, List<? extends Object> items) {
        final String sql = """
                insert into softfruver.compra_item (compra_id, producto_id, cantidad_kg, precio_unit)
                values (?, ?, ?, ?)
                """;
        try {
            jdbc.batchUpdate(sql, items, items.size(), (ps, obj) -> {
                Long productoId;
                BigDecimal cantidadKg;
                BigDecimal precioUnit;

                if (obj instanceof NuevaCompraItem it) {
                    productoId = it.productoId();
                    cantidadKg = it.cantidadKg();
                    precioUnit = it.precioUnit();
                } else if (obj instanceof com.softfruver.inventario.service.ComprasService.ItemNuevaCompra it) {
                    productoId = it.productoId();
                    cantidadKg = it.cantidadKg();
                    precioUnit = it.precioUnit();
                } else {
                    throw new IllegalArgumentException("Tipo de item no soportado: " + obj);
                }

                ps.setLong(1, compraId);
                ps.setLong(2, productoId);
                ps.setBigDecimal(3, cantidadKg);
                ps.setBigDecimal(4, precioUnit);
            });
        } catch (Exception ex) {
            String msg = "Error al insertar ítems de compra. compra_id=" + compraId +
                    ". Causa: " + ex.getClass().getName() + " - " + ex.getMessage();
            throw new RuntimeException(msg, ex);
        }
    }

    // ====== DELETE DETALLE / CABECERA ======
    public void deleteItems(Long compraId) {
        jdbc.update("delete from softfruver.compra_item where compra_id = ?", compraId);
    }

    public void deleteCabecera(Long compraId) {
        jdbc.update("delete from softfruver.compra where id = ?", compraId);
    }
}
