package com.softfruver.inventario.repository;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@Repository
public class VentasRepository {

    private final NamedParameterJdbcTemplate jdbcNamed;
    private final JdbcTemplate jdbc;

    public VentasRepository(NamedParameterJdbcTemplate jdbcNamed, JdbcTemplate jdbc) {
        this.jdbcNamed = jdbcNamed;
        this.jdbc = jdbc;
    }

    // ====== Proyecciones ======
    // Traemos el ID para acciones en la lista
    public record VentaItemListado(Long id, LocalDate fecha, String cliente, String productos, BigDecimal total,
                                   boolean esCredito) {}

    public record VentaDetalleItem(Long productoId, String producto, BigDecimal cantidadKg, BigDecimal precioUnitario) {}

    public record Opcion(Long id, String nombre) {}

    // >>> AQUI exponemos clienteId en la cabecera <<<
    public record VentaCabecera(Long id, LocalDate fecha, Long clienteId, String cliente,
                                BigDecimal total, boolean esCredito) {}

    public record NuevaVentaItem(Long productoId, BigDecimal cantidadKg, BigDecimal precioUnit) {}

    // ====== LISTA ======
    public List<VentaItemListado> buscarVentas(LocalDate desde, LocalDate hasta, String clienteNombre) {
        String sql = """
                select
                  v.id                                      as id,
                  cast(v.fecha_venta as date)               as fecha,
                  c.nombre                                  as cliente,
                  coalesce(
                    string_agg(
                      (p.nombre || ' × ' || trim(to_char(vi.cantidad_kg, 'FM999999990.###')) || ' kg'),
                      ' · ' order by p.nombre
                    ), '—'
                  )                                         as productos,
                  v.total                                   as total,
                  v.es_credito                              as es_credito
                from softfruver.venta v
                join softfruver.cliente c on c.id = v.cliente_id
                left join softfruver.venta_item vi on vi.venta_id = v.id
                left join softfruver.producto p on p.id = vi.producto_id
                where
                  v.fecha_venta >= coalesce(cast(:desde as timestamp), '-infinity'::timestamp) and
                  v.fecha_venta <  coalesce(cast(:hasta as timestamp) + interval '1 day', 'infinity'::timestamp) and
                  (coalesce(:cli,'') = '' or c.nombre ilike concat('%', :cli, '%'))
                group by v.id, cast(v.fecha_venta as date), c.nombre, v.total, v.es_credito
                order by cast(v.fecha_venta as date) desc, v.id desc
                """;

        var params = new MapSqlParameterSource()
                .addValue("desde", desde)
                .addValue("hasta", hasta)
                .addValue("cli", (clienteNombre == null || clienteNombre.isBlank()) ? "" : clienteNombre.trim());

        return jdbcNamed.query(sql, params, (ResultSet rs, int rowNum) -> new VentaItemListado(
                rs.getLong("id"),
                rs.getDate("fecha").toLocalDate(),
                rs.getString("cliente"),
                rs.getString("productos"),
                rs.getBigDecimal("total"),
                rs.getBoolean("es_credito")));
    }

    // ====== CABECERA / DETALLE ======
    public VentaCabecera obtenerCabecera(Long ventaId) {
        String sql = """
                select v.id,
                       cast(v.fecha_venta as date) as fecha,
                       v.cliente_id                 as cliente_id,
                       c.nombre                     as cliente,
                       v.total,
                       v.es_credito
                from softfruver.venta v
                join softfruver.cliente c on c.id = v.cliente_id
                where v.id = :id
                """;
        var params = new MapSqlParameterSource().addValue("id", ventaId);
        return jdbcNamed.query(sql, params, rs -> rs.next()
                ? new VentaCabecera(
                        rs.getLong("id"),
                        rs.getDate("fecha").toLocalDate(),
                        rs.getLong("cliente_id"),   // << clienteId expuesto
                        rs.getString("cliente"),
                        rs.getBigDecimal("total"),
                        rs.getBoolean("es_credito"))
                : null);
    }

    public List<VentaDetalleItem> obtenerDetalle(Long ventaId) {
        String sql = """
                select
                  vi.producto_id   as "productoId",
                  p.nombre         as "producto",
                  vi.cantidad_kg   as "cantidadKg",
                  vi.precio_unit   as "precioUnitario"
                from softfruver.venta_item vi
                join softfruver.producto p on p.id = vi.producto_id
                where vi.venta_id = :ventaId
                order by p.nombre
                """;
        var params = new MapSqlParameterSource().addValue("ventaId", ventaId);
        return jdbcNamed.query(sql, params, (rs, i) -> new VentaDetalleItem(
                rs.getLong("productoId"),
                rs.getString("producto"),
                rs.getBigDecimal("cantidadKg"),
                rs.getBigDecimal("precioUnitario")));
    }

    // ====== OPCIONES ======
    public List<Opcion> opcionesClientes() {
        String sql = "select id, nombre from softfruver.cliente where archived_at is null order by nombre";
        return jdbcNamed.query(sql, (rs, i) -> new Opcion(rs.getLong("id"), rs.getString("nombre")));
    }

    public List<Opcion> opcionesProductos() {
        String sql = "select id, nombre from softfruver.producto where archived_at is null order by nombre";
        return jdbcNamed.query(sql, (rs, i) -> new Opcion(rs.getLong("id"), rs.getString("nombre")));
    }

    // ====== INSERT CABECERA ======
    public Long insertCabecera(Long clienteId, LocalDate fecha, boolean esCredito) {
        try {
            var inserter = new SimpleJdbcInsert(jdbc)
                    .withSchemaName("softfruver")
                    .withTableName("venta")
                    .usingGeneratedKeyColumns("id");

            var now = Timestamp.from(Instant.now());
            var params = new HashMap<String, Object>();
            params.put("cliente_id", clienteId);
            params.put("es_credito", esCredito);
            params.put("total", BigDecimal.ZERO);
            params.put("fecha_venta", Timestamp.valueOf(fecha.atStartOfDay()));
            // columnas NOT NULL en tu BD:
            params.put("creado_en", now);
            params.put("actualizado_en", now);

            Number key = inserter.executeAndReturnKey(new MapSqlParameterSource(params));
            if (key == null)
                throw new IllegalStateException("executeAndReturnKey() devolvió null");
            return key.longValue();

        } catch (Exception ex) {
            String msg = "Error al insertar/actualizar cabecera de venta. cliente_id=" +
                    clienteId + ", fecha=" + fecha + ". Causa: " + ex.getClass().getName() + " - " + ex.getMessage();
            throw new RuntimeException(msg, ex);
        }
    }

    // ====== INSERT ITEMS ======
    public void insertItems(Long ventaId, List<? extends Object> items) {
        final String sql = """
                insert into softfruver.venta_item (venta_id, producto_id, cantidad_kg, precio_unit)
                values (?, ?, ?, ?)
                """;
        try {
            jdbc.batchUpdate(sql, items, items.size(), (ps, obj) -> {
                Long productoId;
                BigDecimal cantidadKg;
                BigDecimal precioUnit;

                if (obj instanceof NuevaVentaItem it) {
                    productoId = it.productoId();
                    cantidadKg = it.cantidadKg();
                    precioUnit = it.precioUnit();
                } else if (obj instanceof com.softfruver.inventario.service.VentasService.ItemNuevaVenta it) {
                    productoId = it.productoId();
                    cantidadKg = it.cantidadKg();
                    precioUnit = it.precioKg(); // mismo valor
                } else {
                    throw new IllegalArgumentException("Tipo de item no soportado: " + obj);
                }

                ps.setLong(1, ventaId);
                ps.setLong(2, productoId);
                ps.setBigDecimal(3, cantidadKg);
                ps.setBigDecimal(4, precioUnit);
            });

        } catch (DataAccessException ex) {
            Throwable root = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause() : ex;
            String rootMsg = root.getMessage();
            String msg = "Error al insertar ítems de venta. venta_id=" + ventaId +
                    ". Detalle: " + root.getClass().getSimpleName() + (rootMsg != null ? " - " + rootMsg : "");
            throw new RuntimeException(msg, ex);
        } catch (Exception ex) {
            String msg = "Error al insertar ítems de venta. venta_id=" + ventaId + ". Causa: " +
                    ex.getClass().getName() + " - " + ex.getMessage();
            throw new RuntimeException(msg, ex);
        }
    }

    // ====== UPDATE / DELETE ======
    public void updateCabecera(Long ventaId, Long clienteId, LocalDate fecha, boolean esCredito) {
        final String sql = """
                  update softfruver.venta
                     set cliente_id = ?,
                         es_credito = ?,
                         fecha_venta = ?,
                         actualizado_en = now()
                   where id = ?
                """;
        jdbc.update(sql,
                clienteId,
                esCredito,
                Timestamp.valueOf(fecha.atStartOfDay()),
                ventaId);
    }

    public void deleteItems(Long ventaId) {
        jdbc.update("delete from softfruver.venta_item where venta_id = ?", ventaId);
    }

    public void deleteCabecera(Long ventaId) {
        jdbc.update("delete from softfruver.venta where id = ?", ventaId);
    }
}
