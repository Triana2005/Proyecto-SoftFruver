package com.softfruver.inventario.service;

import com.softfruver.inventario.model.Cliente;
import com.softfruver.inventario.repository.ClienteRepository;
import com.softfruver.inventario.repository.projection.ClienteListado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ClienteService {

  private final ClienteRepository repo;

  public ClienteService(ClienteRepository repo) {
    this.repo = repo;
  }

  // Listar activos por defecto
  @Transactional(readOnly = true)
  public List<ClienteListado> listar() {
    return repo.listarVisiblesOrden();
  }

  // Buscar según estado (activos/archivados)
  @Transactional(readOnly = true)
  public List<ClienteListado> buscarPorEstado(boolean archivados, String q) {
    if (q == null || q.isBlank()) {
      return archivados ? repo.listarArchivadosOrden() : repo.listarVisiblesOrden();
    }
    String term = q.trim();
    return archivados ? repo.buscarArchivados(term) : repo.buscarActivos(term);
  }

  @Transactional
  public Cliente crear(String nombre, String telefono) {
    String nombreTrim = nombre == null ? "" : nombre.trim();
    if (nombreTrim.isEmpty()) {
      throw new IllegalArgumentException("El nombre es obligatorio.");
    }

    // Verifica duplicado de nombre (ignorando tildes y mayúsculas)
    if (repo.existsNombreNormalizado(nombreTrim)) {
      throw new IllegalArgumentException(
          "Ya existe un cliente activo con ese nombre (se ignoran tildes y mayúsculas).");
    }

    // Verifica duplicado de teléfono (si no está vacío)
    if (telefono != null && !telefono.trim().isEmpty()) {
      String telTrim = telefono.trim();
      if (repo.existsTelefonoActivo(telTrim)) {
        throw new IllegalArgumentException("Ya existe un cliente activo con ese número de teléfono.");
      }
    }

    Cliente c = new Cliente();
    c.setNombre(nombreTrim);
    c.setTelefono(telefono != null ? telefono.trim() : null);
    c.setCreadoEn(OffsetDateTime.now());
    c.setActualizadoEn(OffsetDateTime.now());
    return repo.save(c);
  }

  @Transactional
  public void archivar(Long id) {
    Cliente c = repo.findById(id).orElseThrow();
    c.setArchivedAt(OffsetDateTime.now());
    c.setActualizadoEn(OffsetDateTime.now());
    repo.save(c);
  }

  @Transactional
  public void restaurar(Long id) {
    Cliente c = repo.findById(id).orElseThrow();
    c.setArchivedAt(null);
    c.setActualizadoEn(OffsetDateTime.now());
    repo.save(c);
  }
}
