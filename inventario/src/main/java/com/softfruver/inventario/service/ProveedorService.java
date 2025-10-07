package com.softfruver.inventario.service;

import com.softfruver.inventario.model.Proveedor;
import com.softfruver.inventario.repository.ProveedorRepository;
import com.softfruver.inventario.repository.projection.ProveedorListado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class ProveedorService {

  private final ProveedorRepository repo;

  public ProveedorService(ProveedorRepository repo) {
    this.repo = repo;
  }

  // ===== Listado paginado con búsqueda =====
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
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

  // ===== Crear / Archivar / Restaurar =====

  @org.springframework.transaction.annotation.Transactional
  public void crear(String nombre, String telefono) {
    if (nombre == null || nombre.isBlank()) {
      throw new IllegalArgumentException("El nombre es obligatorio.");
    }
    Proveedor p = new Proveedor();
    p.setNombre(nombre.trim());
    if (telefono != null) p.setTelefono(telefono.trim());
    p.setArchivedAt(null);
    p.setCreadoEn(OffsetDateTime.now());
    p.setActualizadoEn(OffsetDateTime.now());
    repo.save(p);
  }

  @org.springframework.transaction.annotation.Transactional
  public void archivar(Long id) {
    Proveedor p = repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
    if (p.getArchivedAt() == null) {
      p.setArchivedAt(OffsetDateTime.now());
      p.setActualizadoEn(OffsetDateTime.now());
      repo.save(p);
    }
  }

  @org.springframework.transaction.annotation.Transactional
  public void restaurar(Long id) {
    Proveedor p = repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
    if (p.getArchivedAt() != null) {
      p.setArchivedAt(null);
      p.setActualizadoEn(OffsetDateTime.now());
      repo.save(p);
    }
  }
}
