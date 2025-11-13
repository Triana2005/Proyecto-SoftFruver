package com.softfruver.inventario.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

@Repository
public class DashboardRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public DashboardRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  // --- COMPRAS ---
  public long contarCompras(Instant ini, Instant fin) {
    String sql = """
      SELECT COUNT(*) 
      FROM softfruver.compra 
      WHERE fecha_compra >= :ini AND fecha_compra < :fin
    """;
    return jdbc.queryForObject(sql, Map.of("ini", Timestamp.from(ini), "fin", Timestamp.from(fin)), Long.class);
  }

  public BigDecimal totalCompras(Instant ini, Instant fin) {
    String sql = """
      SELECT COALESCE(SUM(total),0) 
      FROM softfruver.compra 
      WHERE fecha_compra >= :ini AND fecha_compra < :fin
    """;
    return jdbc.queryForObject(sql, Map.of("ini", Timestamp.from(ini), "fin", Timestamp.from(fin)), BigDecimal.class);
  }

  // --- VENTAS ---
  public long contarVentas(Instant ini, Instant fin) {
    String sql = """
      SELECT COUNT(*) 
      FROM softfruver.venta 
      WHERE fecha_venta >= :ini AND fecha_venta < :fin
    """;
    return jdbc.queryForObject(sql, Map.of("ini", Timestamp.from(ini), "fin", Timestamp.from(fin)), Long.class);
  }

  public BigDecimal totalVentas(Instant ini, Instant fin) {
    String sql = """
      SELECT COALESCE(SUM(total),0) 
      FROM softfruver.venta 
      WHERE fecha_venta >= :ini AND fecha_venta < :fin
    """;
    return jdbc.queryForObject(sql, Map.of("ini", Timestamp.from(ini), "fin", Timestamp.from(fin)), BigDecimal.class);
  }

  // --- PAGOS: cliente + proveedor ---
  public long contarPagos(Instant ini, Instant fin) {
    String sql = """
      SELECT 
        (SELECT COUNT(*) FROM softfruver.pago_cliente   WHERE fecha_pago >= :ini AND fecha_pago < :fin)
      + (SELECT COUNT(*) FROM softfruver.pago_proveedor WHERE fecha_pago >= :ini AND fecha_pago < :fin)
    """;
    return jdbc.queryForObject(sql, Map.of("ini", Timestamp.from(ini), "fin", Timestamp.from(fin)), Long.class);
  }

  public BigDecimal totalPagos(Instant ini, Instant fin) {
    String sql = """
      SELECT 
        COALESCE((SELECT SUM(monto) FROM softfruver.pago_cliente   WHERE fecha_pago >= :ini AND fecha_pago < :fin),0)
      + COALESCE((SELECT SUM(monto) FROM softfruver.pago_proveedor WHERE fecha_pago >= :ini AND fecha_pago < :fin),0)
    """;
    return jdbc.queryForObject(sql, Map.of("ini", Timestamp.from(ini), "fin", Timestamp.from(fin)), BigDecimal.class);
  }
}
