package com.softfruver.inventario.repository;

import com.softfruver.inventario.model.Cliente;
import com.softfruver.inventario.repository.projection.ClienteListado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

  // Lista visible (ignora archivados) ordenada por saldo desc, nombre asc
  @Query(value = """
      SELECT c.id AS id,
             c.nombre AS nombre,
             c.telefono AS telefono,
             COALESCE(vs.saldo_total,0) AS saldoTotal
      FROM softfruver.cliente c
      LEFT JOIN softfruver.v_saldo_cliente vs ON vs.cliente_id = c.id
      WHERE c.archived_at IS NULL
      ORDER BY COALESCE(vs.saldo_total,0) DESC, c.nombre ASC
      """,
      nativeQuery = true)
  List<ClienteListado> listarVisiblesOrden();

  // Búsqueda simple por nombre
  @Query(value = """
      SELECT c.id AS id,
             c.nombre AS nombre,
             c.telefono AS telefono,
             COALESCE(vs.saldo_total,0) AS saldoTotal
      FROM softfruver.cliente c
      LEFT JOIN softfruver.v_saldo_cliente vs ON vs.cliente_id = c.id
      WHERE c.archived_at IS NULL
        AND unaccent(lower(c.nombre)) LIKE unaccent(lower(CONCAT('%', :q, '%')))
      ORDER BY COALESCE(vs.saldo_total,0) DESC, c.nombre ASC
      """,
      nativeQuery = true)
  List<ClienteListado> buscar(@Param("q") String q);
}
