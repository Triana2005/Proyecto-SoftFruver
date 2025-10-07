package com.softfruver.inventario.repository;

import com.softfruver.inventario.model.Proveedor;
import com.softfruver.inventario.repository.projection.ProveedorListado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

  // Listados paginados (activos/archivados), orden por deuda desc y nombre
  @Query(value = """
      SELECT p.id AS id, p.nombre AS nombre, p.telefono AS telefono,
             COALESCE(vs.deuda_total,0) AS deudaTotal
      FROM softfruver.proveedor p
      LEFT JOIN softfruver.v_saldo_proveedor vs ON vs.proveedor_id = p.id
      WHERE p.archived_at IS NULL
      ORDER BY COALESCE(vs.deuda_total,0) DESC, p.nombre ASC
      """,
      countQuery = "SELECT COUNT(*) FROM softfruver.proveedor p WHERE p.archived_at IS NULL",
      nativeQuery = true)
  Page<ProveedorListado> listarVisiblesOrdenPage(Pageable pageable);

  @Query(value = """
      SELECT p.id AS id, p.nombre AS nombre, p.telefono AS telefono,
             COALESCE(vs.deuda_total,0) AS deudaTotal
      FROM softfruver.proveedor p
      LEFT JOIN softfruver.v_saldo_proveedor vs ON vs.proveedor_id = p.id
      WHERE p.archived_at IS NOT NULL
      ORDER BY COALESCE(vs.deuda_total,0) DESC, p.nombre ASC
      """,
      countQuery = "SELECT COUNT(*) FROM softfruver.proveedor p WHERE p.archived_at IS NOT NULL",
      nativeQuery = true)
  Page<ProveedorListado> listarArchivadosOrdenPage(Pageable pageable);

  // Búsqueda normalizada (sin tildes / case-insensitive) PAGINADA por nombre
  @Query(value = """
      SELECT p.id AS id, p.nombre AS nombre, p.telefono AS telefono,
             COALESCE(vs.deuda_total,0) AS deudaTotal
      FROM softfruver.proveedor p
      LEFT JOIN softfruver.v_saldo_proveedor vs ON vs.proveedor_id = p.id
      WHERE p.archived_at IS NULL
        AND softfruver.imm_unaccent(lower(p.nombre))
            LIKE CONCAT('%', softfruver.imm_unaccent(lower(:q)), '%')
      ORDER BY COALESCE(vs.deuda_total,0) DESC, p.nombre ASC
      """,
      countQuery = """
      SELECT COUNT(*)
      FROM softfruver.proveedor p
      WHERE p.archived_at IS NULL
        AND softfruver.imm_unaccent(lower(p.nombre))
            LIKE CONCAT('%', softfruver.imm_unaccent(lower(:q)), '%')
      """,
      nativeQuery = true)
  Page<ProveedorListado> buscarActivosPage(@Param("q") String q, Pageable pageable);

  @Query(value = """
      SELECT p.id AS id, p.nombre AS nombre, p.telefono AS telefono,
             COALESCE(vs.deuda_total,0) AS deudaTotal
      FROM softfruver.proveedor p
      LEFT JOIN softfruver.v_saldo_proveedor vs ON vs.proveedor_id = p.id
      WHERE p.archived_at IS NOT NULL
        AND softfruver.imm_unaccent(lower(p.nombre))
            LIKE CONCAT('%', softfruver.imm_unaccent(lower(:q)), '%')
      ORDER BY COALESCE(vs.deuda_total,0) DESC, p.nombre ASC
      """,
      countQuery = """
      SELECT COUNT(*)
      FROM softfruver.proveedor p
      WHERE p.archived_at IS NOT NULL
        AND softfruver.imm_unaccent(lower(p.nombre))
            LIKE CONCAT('%', softfruver.imm_unaccent(lower(:q)), '%')
      """,
      nativeQuery = true)
  Page<ProveedorListado> buscarArchivadosPage(@Param("q") String q, Pageable pageable);
}
