package com.softfruver.inventario.service;

import com.softfruver.inventario.model.Proveedor;
import com.softfruver.inventario.repository.ProveedorRepository;
import com.softfruver.inventario.repository.projection.ProveedorListado;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;

@Service
public class ProveedorService {

  private final ProveedorRepository repo;

  public ProveedorService(ProveedorRepository repo) {
    this.repo = repo;
  }

  // ===== Listado paginado con búsqueda =====
  @Transactional(readOnly = true)
  public Page<ProveedorListado> buscarPorEstado(boolean archivados, String q, Pageable pageable) {
    String term = (q == null || q.isBlank()) ? null : q.trim();
    if (term == null) {
      return archivados
          ? repo.listarArchivadosOrdenPage(pageable)
          : repo.listarVisiblesOrdenPage(pageable);
    }
    return archivados
        ? repo.buscarArchivadosPage(term, pageable)
        : repo.buscarActivosPage(term, pageable);
  }

  // ===== Crear =====
  @Transactional
  public void crear(String nombre, String telefono) {
    if (nombre == null || nombre.isBlank()) {
      throw new IllegalArgumentException("El nombre es obligatorio.");
    }
    String nombreTrim = nombre.trim();

    // Validaciones como en Clientes
    if (repo.existsNombreNormalizado(nombreTrim)) {
      throw new IllegalArgumentException(
          "Ya existe un proveedor activo con ese nombre (se ignoran tildes y mayúsculas).");
    }
    if (telefono != null && !telefono.trim().isEmpty()) {
      String telTrim = telefono.trim();
      if (repo.existsTelefonoActivo(telTrim)) {
        throw new IllegalArgumentException("Ya existe un proveedor activo con ese teléfono.");
      }
    }

    Proveedor p = new Proveedor();
    p.setNombre(nombreTrim);
    p.setTelefono(telefono != null ? telefono.trim() : null);
    p.setArchivedAt(null);
    p.setCreadoEn(OffsetDateTime.now());
    p.setActualizadoEn(OffsetDateTime.now());
    repo.save(p);
  }

  // ===== Archivar / Restaurar =====
  @Transactional
  public void archivar(Long id) {
    Proveedor p = repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
    if (p.getArchivedAt() == null) {
      p.setArchivedAt(OffsetDateTime.now());
      p.setActualizadoEn(OffsetDateTime.now());
      repo.save(p);
    }
  }

  @Transactional
  public void restaurar(Long id) {
    Proveedor p = repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
    if (p.getArchivedAt() != null) {
      p.setArchivedAt(null);
      p.setActualizadoEn(OffsetDateTime.now());
      repo.save(p);
    }
  }

  // ===== Obtener y Actualizar (para "Modificar") =====
  @Transactional(readOnly = true)
  public Proveedor getById(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado."));
  }

  @Transactional
  public void actualizar(Long id, String nombre, String telefono) {
    if (nombre == null || nombre.isBlank()) {
      throw new IllegalArgumentException("El nombre es obligatorio.");
    }
    String nombreLimpio = nombre.trim();
    String tel = (telefono == null ? null : telefono.trim());

    Proveedor p = getById(id);

    boolean cambioNombre = !p.getNombre().equalsIgnoreCase(nombreLimpio);
    boolean cambioTel = !Objects.equals(
        p.getTelefono() == null ? null : p.getTelefono().trim(),
        tel == null ? null : (tel.isBlank() ? null : tel)
    );

    if (!cambioNombre && !cambioTel) return;

    // Si cambió el nombre, validar unicidad (como en crear)
    if (cambioNombre && repo.existsNombreNormalizado(nombreLimpio)) {
      throw new DataIntegrityViolationException("Nombre duplicado");
    }
    if (cambioTel && tel != null && !tel.isBlank() && repo.existsTelefonoActivo(tel)) {
      throw new DataIntegrityViolationException("Teléfono duplicado");
    }

    p.setNombre(nombreLimpio);
    p.setTelefono((tel != null && tel.isBlank()) ? null : tel);
    p.setActualizadoEn(OffsetDateTime.now());
    repo.save(p);
  }
}
