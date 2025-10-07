package com.softfruver.inventario.repository;

import com.softfruver.inventario.model.Cliente;
import com.softfruver.inventario.repository.projection.ClienteListado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

  // --- Listados ---
  @Query(value = """
      SELECT c.id AS id, c.nombre AS nombre, c.telefono AS telefono,
             COALESCE(vs.saldo_total,0) AS saldoTotal
      FROM softfruver.cliente c
      LEFT JOIN softfruver.v_saldo_cliente vs ON vs.cliente_id = c.id
      WHERE c.archived_at IS NULL
      ORDER BY COALESCE(vs.saldo_total,0) DESC, c.nombre ASC
      """, nativeQuery = true)
  List<ClienteListado> listarVisiblesOrden();

  @Query(value = """
      SELECT c.id AS id, c.nombre AS nombre, c.telefono AS telefono,
             COALESCE(vs.saldo_total,0) AS saldoTotal
      FROM softfruver.cliente c
      LEFT JOIN softfruver.v_saldo_cliente vs ON vs.cliente_id = c.id
      WHERE c.archived_at IS NOT NULL
      ORDER BY c.actualizado_en DESC, c.nombre ASC
      """, nativeQuery = true)
  List<ClienteListado> listarArchivadosOrden();

  // --- Búsquedas (sin unaccent) ---
  @Query(value = """
      SELECT c.id AS id, c.nombre AS nombre, c.telefono AS telefono,
             COALESCE(vs.saldo_total,0) AS saldoTotal
      FROM softfruver.cliente c
      LEFT JOIN softfruver.v_saldo_cliente vs ON vs.cliente_id = c.id
      WHERE c.archived_at IS NULL
        AND c.nombre ILIKE CONCAT('%', :q, '%')
      ORDER BY COALESCE(vs.saldo_total,0) DESC, c.nombre ASC
      """, nativeQuery = true)
  List<ClienteListado> buscarActivos(@Param("q") String q);

  @Query(value = """
      SELECT c.id AS id, c.nombre AS nombre, c.telefono AS telefono,
             COALESCE(vs.saldo_total,0) AS saldoTotal
      FROM softfruver.cliente c
      LEFT JOIN softfruver.v_saldo_cliente vs ON vs.cliente_id = c.id
      WHERE c.archived_at IS NOT NULL
        AND c.nombre ILIKE CONCAT('%', :q, '%')
      ORDER BY c.actualizado_en DESC, c.nombre ASC
      """, nativeQuery = true)
  List<ClienteListado> buscarArchivados(@Param("q") String q);

  @Query(value = """
      SELECT c.id AS id,
             c.nombre AS nombre,
             c.telefono AS telefono,
             COALESCE(vs.saldo_total,0) AS saldoTotal
      FROM softfruver.cliente c
      LEFT JOIN softfruver.v_saldo_cliente vs ON vs.cliente_id = c.id
      WHERE c.archived_at IS NULL
        AND lower(softfruver.f_unaccent(c.nombre)) LIKE lower(softfruver.f_unaccent(CONCAT('%', :q, '%')))
      ORDER BY COALESCE(vs.saldo_total,0) DESC, c.nombre ASC
      """, nativeQuery = true)
  List<ClienteListado> buscar(@Param("q") String q);

  @Query(value = """
      SELECT COUNT(*) > 0
      FROM softfruver.cliente c
      WHERE c.archived_at IS NULL
        AND lower(softfruver.f_unaccent(c.nombre)) = lower(softfruver.f_unaccent(:nombre))
      """, nativeQuery = true)
  boolean existsNombreNormalizado(@Param("nombre") String nombre);

  @Query(value = """
  SELECT COUNT(*) > 0
  FROM softfruver.cliente c
  WHERE c.archived_at IS NULL
    AND c.telefono IS NOT NULL
    AND c.telefono = :telefono
  """, nativeQuery = true)
boolean existsTelefonoActivo(@Param("telefono") String telefono);


}
