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

  @Transactional(readOnly = true)
  public List<ClienteListado> listar() {
    return repo.listarVisiblesOrden();
  }

  @Transactional(readOnly = true)
  public List<ClienteListado> buscar(String q) {
    if (q == null || q.isBlank()) return listar();
    return repo.buscar(q.trim());
  }

  @Transactional
  public Cliente crear(String nombre, String telefono) {
    Cliente c = new Cliente();
    c.setNombre(nombre.trim());
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
}
