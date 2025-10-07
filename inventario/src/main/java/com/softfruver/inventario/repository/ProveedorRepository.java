package com.softfruver.inventario.repository;

import com.softfruver.inventario.model.Proveedor;
import com.softfruver.inventario.repository.projection.ProveedorListado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

  // === Listados paginados (activos / archivados) ===
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

  // === Búsqueda normalizada (sin tildes / case-insensitive) PAGINADA por nombre ===
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

  //  Unicidad 
  @Query(value = """
      SELECT COUNT(*) > 0
      FROM softfruver.proveedor p
      WHERE p.archived_at IS NULL
        AND lower(softfruver.f_unaccent(p.nombre)) = lower(softfruver.f_unaccent(:nombre))
      """, nativeQuery = true)
  boolean existsNombreNormalizado(@Param("nombre") String nombre);

  @Query(value = """
      SELECT COUNT(*) > 0
      FROM softfruver.proveedor p
      WHERE p.archived_at IS NULL
        AND p.telefono IS NOT NULL
        AND p.telefono = :telefono
      """, nativeQuery = true)
  boolean existsTelefonoActivo(@Param("telefono") String telefono);
}
