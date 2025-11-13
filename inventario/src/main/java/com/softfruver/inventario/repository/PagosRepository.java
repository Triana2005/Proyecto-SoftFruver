package com.softfruver.inventario.repository;

import org.postgresql.util.PGobject;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class PagosRepository {

    private final NamedParameterJdbcTemplate jdbcNamed;
    private final JdbcTemplate jdbc;

    // cache simple (UPPER -> etiqueta exacta del enum)
    private volatile Map<String, String> metodoPagoMapUpperToExact;

    public PagosRepository(NamedParameterJdbcTemplate jdbcNamed, JdbcTemplate jdbc) {
        this.jdbcNamed = jdbcNamed;
        this.jdbc = jdbc;
    }

    // ====== Proyecciones ======
    public record Opcion(Long id, String nombre) {}
    public record PagoItemListado(Long id, LocalDate fecha, String tipo, String refNombre,
                                  BigDecimal monto, String metodo) {}
    public record PagoCabecera(Long id, LocalDate fecha, String tipo,
                               Long refId, String refNombre, BigDecimal monto, String metodo) {}

    // ====== Helpers ======
    private Timestamp ts(LocalDate fecha) {
        return Timestamp.valueOf(fecha.atStartOfDay());
    }

    private Map<String, String> loadMetodoPagoMap() {
        if (metodoPagoMapUpperToExact != null) return metodoPagoMapUpperToExact;
        synchronized (this) {
            if (metodoPagoMapUpperToExact == null) {
                List<String> labels = jdbc.query(
                        """
                        select enumlabel
                          from pg_enum e
                          join pg_type t on t.oid = e.enumtypid
                         where t.typname = 'metodo_pago'
                           and t.typnamespace = 'softfruver'::regnamespace
                         order by e.enumsortorder
                        """,
                        (rs, i) -> rs.getString("enumlabel")
                );
                Map<String, String> map = new LinkedHashMap<>();
                for (String lbl : labels) map.put(lbl.toUpperCase(Locale.ROOT), lbl);
                metodoPagoMapUpperToExact = map;
            }
        }
        return metodoPagoMapUpperToExact;
    }

    private List<String> loadMetodoPagoLabels() {
        return new ArrayList<>(loadMetodoPagoMap().values());
    }

    private String normalizeMetodo(String metodoRaw) {
        if (metodoRaw == null) throw new IllegalArgumentException("El campo 'metodo' es obligatorio.");
        String exact = loadMetodoPagoMap().get(metodoRaw.trim().toUpperCase(Locale.ROOT));
        if (exact == null) {
            String lista = loadMetodoPagoLabels().stream().collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Valor de 'metodo' inválido: '" + metodoRaw + "'. Valores permitidos: " + lista);
        }
        return exact;
    }

    /** Crea un PGobject tipado al enum para evitar casts textuales en SQL. */
    private PGobject pgEnumMetodo(String exactLabel) {
        try {
            PGobject o = new PGobject();
            o.setType("softfruver.metodo_pago"); // OJO: tipo totalmente calificado
            o.setValue(exactLabel);
            return o;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo crear PGobject para metodo_pago", e);
        }
    }

    // ====== LISTA ======
    public List<PagoItemListado> buscarPagos(LocalDate desde, LocalDate hasta, String refNombre, String tipo) {
        String filtroCli = (tipo == null || tipo.isBlank() || "CLIENTE".equals(tipo)) ? """
                SELECT pc.id,
                       cast(pc.fecha_pago as date) as fecha,
                       'CLIENTE' as tipo,
                       c.nombre as ref_nombre,
                       pc.monto,
                       pc.metodo::text as metodo
                  FROM softfruver.pago_cliente pc
                  JOIN softfruver.cliente c ON c.id = pc.cliente_id
                 WHERE pc.fecha_pago >= coalesce(cast(:desde as timestamp), '-infinity'::timestamp)
                   AND pc.fecha_pago <  coalesce(cast(:hasta as timestamp) + interval '1 day', 'infinity'::timestamp)
                   AND (coalesce(:ref,'') = '' OR c.nombre ilike concat('%', :ref, '%'))
            """ : "";

        String filtroProv = (tipo == null || tipo.isBlank() || "PROVEEDOR".equals(tipo)) ? """
                SELECT pp.id,
                       cast(pp.fecha_pago as date) as fecha,
                       'PROVEEDOR' as tipo,
                       p.nombre as ref_nombre,
                       pp.monto,
                       pp.metodo::text as metodo
                  FROM softfruver.pago_proveedor pp
                  JOIN softfruver.proveedor p ON p.id = pp.proveedor_id
                 WHERE pp.fecha_pago >= coalesce(cast(:desde as timestamp), '-infinity'::timestamp)
                   AND pp.fecha_pago <  coalesce(cast(:hasta as timestamp) + interval '1 day', 'infinity'::timestamp)
                   AND (coalesce(:ref,'') = '' OR p.nombre ilike concat('%', :ref, '%'))
            """ : "";

        String sql;
        if (!filtroCli.isBlank() && !filtroProv.isBlank()) {
            sql = filtroCli + "\nUNION ALL\n" + filtroProv + "\nORDER BY fecha DESC, id DESC";
        } else if (!filtroCli.isBlank()) {
            sql = filtroCli + "\nORDER BY fecha DESC, id DESC";
        } else {
            sql = filtroProv + "\nORDER BY fecha DESC, id DESC";
        }

        var params = new MapSqlParameterSource()
                .addValue("desde", desde)
                .addValue("hasta", hasta)
                .addValue("ref", (refNombre == null ? "" : refNombre.trim()));

        return jdbcNamed.query(sql, params, (ResultSet rs, int rowNum) -> new PagoItemListado(
                rs.getLong("id"),
                rs.getDate("fecha").toLocalDate(),
                rs.getString("tipo"),
                rs.getString("ref_nombre"),
                rs.getBigDecimal("monto"),
                rs.getString("metodo")
        ));
    }

    // ====== CABECERA ======
    public PagoCabecera obtenerCabecera(Long id) {
        String sql = """
            SELECT id, fecha, tipo, ref_id, ref_nombre, monto, metodo FROM (
              SELECT pc.id,
                     cast(pc.fecha_pago as date) as fecha,
                     'CLIENTE' as tipo,
                     pc.cliente_id as ref_id,
                     c.nombre as ref_nombre,
                     pc.monto,
                     pc.metodo::text as metodo
                FROM softfruver.pago_cliente pc
                JOIN softfruver.cliente c ON c.id = pc.cliente_id
               WHERE pc.id = :id
              UNION ALL
              SELECT pp.id,
                     cast(pp.fecha_pago as date) as fecha,
                     'PROVEEDOR' as tipo,
                     pp.proveedor_id as ref_id,
                     p.nombre as ref_nombre,
                     pp.monto,
                     pp.metodo::text as metodo
                FROM softfruver.pago_proveedor pp
                JOIN softfruver.proveedor p ON p.id = pp.proveedor_id
               WHERE pp.id = :id
            ) t
            """;
        var params = new MapSqlParameterSource().addValue("id", id);
        return jdbcNamed.query(sql, params, rs -> rs.next()
                ? new PagoCabecera(
                        rs.getLong("id"),
                        rs.getDate("fecha").toLocalDate(),
                        rs.getString("tipo"),
                        rs.getLong("ref_id"),
                        rs.getString("ref_nombre"),
                        rs.getBigDecimal("monto"),
                        rs.getString("metodo"))
                : null);
    }

    // ====== OPCIONES ======
    public List<Opcion> opcionesClientes() {
        return jdbcNamed.query(
                "select id, nombre from softfruver.cliente where archived_at is null order by nombre",
                (rs, i) -> new Opcion(rs.getLong("id"), rs.getString("nombre")));
    }

    public List<Opcion> opcionesProveedores() {
        return jdbcNamed.query(
                "select id, nombre from softfruver.proveedor where archived_at is null order by nombre",
                (rs, i) -> new Opcion(rs.getLong("id"), rs.getString("nombre")));
    }

    // ====== INSERTS (sin casts textuales) ======
    public Long insertPagoCliente(Long clienteId, LocalDate fecha, BigDecimal monto, String metodoRaw) {
        final String metodoExact = normalizeMetodo(metodoRaw);
        final String sql = """
            insert into softfruver.pago_cliente
                (cliente_id, monto, metodo, fecha_pago, creado_en, actualizado_en)
            values
                (?, ?, ?, ?, now(), now())
            returning id
        """;
        try {
            return jdbc.query(con -> {
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setLong(1, clienteId);
                ps.setBigDecimal(2, monto);
                ps.setObject(3, pgEnumMetodo(metodoExact)); // <- enum tipado
                ps.setTimestamp(4, ts(fecha));
                return ps;
            }, rs -> {
                if (rs.next()) return rs.getLong(1);
                throw new IllegalStateException("No se devolvió el id del pago_cliente");
            });
        } catch (Exception ex) {
            throw wrap("cliente", "cliente_id=" + clienteId, fecha, ex);
        }
    }

    public Long insertPagoProveedor(Long proveedorId, LocalDate fecha, BigDecimal monto, String metodoRaw) {
        final String metodoExact = normalizeMetodo(metodoRaw);
        final String sql = """
            insert into softfruver.pago_proveedor
                (proveedor_id, monto, metodo, fecha_pago, creado_en, actualizado_en)
            values
                (?, ?, ?, ?, now(), now())
            returning id
        """;
        try {
            return jdbc.query(con -> {
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setLong(1, proveedorId);
                ps.setBigDecimal(2, monto);
                ps.setObject(3, pgEnumMetodo(metodoExact)); // <- enum tipado
                ps.setTimestamp(4, ts(fecha));
                return ps;
            }, rs -> {
                if (rs.next()) return rs.getLong(1);
                throw new IllegalStateException("No se devolvió el id del pago_proveedor");
            });
        } catch (Exception ex) {
            throw wrap("proveedor", "proveedor_id=" + proveedorId, fecha, ex);
        }
    }

    // ====== UPDATES ======
    public void updatePagoCliente(Long id, Long clienteId, LocalDate fecha, BigDecimal monto, String metodoRaw) {
        final String metodoExact = normalizeMetodo(metodoRaw);
        final String sql = """
              update softfruver.pago_cliente
                 set cliente_id = ?,
                     fecha_pago = ?,
                     monto = ?,
                     metodo = ?,
                     actualizado_en = now()
               where id = ?
            """;
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setLong(1, clienteId);
                ps.setTimestamp(2, ts(fecha));
                ps.setBigDecimal(3, monto);
                ps.setObject(4, pgEnumMetodo(metodoExact)); // <- enum tipado
                ps.setLong(5, id);
                return ps;
            });
        } catch (Exception ex) {
            throw new RuntimeException("Error al actualizar pago de cliente. id=" + id + ". " +
                    ex.getClass().getSimpleName() + " - " + ex.getMessage(), ex);
        }
    }

    public void updatePagoProveedor(Long id, Long proveedorId, LocalDate fecha, BigDecimal monto, String metodoRaw) {
        final String metodoExact = normalizeMetodo(metodoRaw);
        final String sql = """
              update softfruver.pago_proveedor
                 set proveedor_id = ?,
                     fecha_pago = ?,
                     monto = ?,
                     metodo = ?,
                     actualizado_en = now()
               where id = ?
            """;
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setLong(1, proveedorId);
                ps.setTimestamp(2, ts(fecha));
                ps.setBigDecimal(3, monto);
                ps.setObject(4, pgEnumMetodo(metodoExact)); // <- enum tipado
                ps.setLong(5, id);
                return ps;
            });
        } catch (Exception ex) {
            throw new RuntimeException("Error al actualizar pago de proveedor. id=" + id + ". " +
                    ex.getClass().getSimpleName() + " - " + ex.getMessage(), ex);
        }
    }

    // ====== DELETES ======
    public void deletePagoCliente(Long id) {
        jdbc.update("delete from softfruver.pago_cliente where id = ?", id);
    }

    public void deletePagoProveedor(Long id) {
        jdbc.update("delete from softfruver.pago_proveedor where id = ?", id);
    }

    // ====== Util ======
    public String tipoPorId(Long id) {
        try {
            Integer x = jdbc.queryForObject("select 1 from softfruver.pago_cliente where id = ?", Integer.class, id);
            if (x != null) return "CLIENTE";
        } catch (DataAccessException ignore) {}
        try {
            Integer y = jdbc.queryForObject("select 1 from softfruver.pago_proveedor where id = ?", Integer.class, id);
            if (y != null) return "PROVEEDOR";
        } catch (DataAccessException ignore) {}
        return null;
    }

    // ====== Error con más contexto (SQLSTATE) ======
    private RuntimeException wrap(String tipo, String ref, LocalDate fecha, Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) cause = cause.getCause();

        StringBuilder sb = new StringBuilder();
        sb.append("Error al insertar pago de ").append(tipo)
          .append(". ").append(ref).append(", fecha=").append(fecha).append(". ");

        if (cause instanceof org.postgresql.util.PSQLException psql) {
            String state = psql.getSQLState();
            String serverMsg = (psql.getServerErrorMessage() != null)
                    ? ("DetallePG=" + psql.getServerErrorMessage())
                    : "sin detalle PG";
            sb.append("SQLSTATE=").append(state).append(". ").append(serverMsg);
        } else {
            sb.append("Causa: ").append(ex.getClass().getSimpleName())
              .append(" - ").append(ex.getMessage());
        }
        return new RuntimeException(sb.toString(), ex);
    }
}
