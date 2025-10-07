package com.softfruver.inventario.controller;

import com.softfruver.inventario.service.ClienteService;
import com.softfruver.inventario.repository.projection.ClienteListado;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
@Validated
@PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
public class ClienteController {

  private final ClienteService service;

  public ClienteController(ClienteService service) {
    this.service = service;
  }

  @GetMapping
  public String listado(
      @RequestParam(value = "estado", required = false) String estado,
      @RequestParam(value = "q", required = false) String q,
      @PageableDefault(size = 10) Pageable pageable,
      Model model
  ) {
    boolean verArchivados = "archivados".equalsIgnoreCase(estado);
    model.addAttribute("estado", verArchivados ? "archivados" : "activos");
    model.addAttribute("q", q);

    Page<ClienteListado> page = service.buscarPorEstado(verArchivados, q, pageable);
    model.addAttribute("page", page);
    return "clientes/lista";
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public String crear(@RequestParam @NotBlank String nombre,
                      @RequestParam(required = false) String telefono,
                      RedirectAttributes ra) {
    try {
      service.crear(nombre, telefono);
      ra.addFlashAttribute("ok", "creado");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("err", ex.getMessage());
    } catch (DataIntegrityViolationException ex) {
      ra.addFlashAttribute("err",
          "Ya existe un cliente activo con ese nombre (ignorando tildes y mayúsculas).");
    }
    return "redirect:/clientes";
  }

  @PostMapping("/{id}/archivar")
  @PreAuthorize("hasRole('ADMIN')")
  public String archivar(@PathVariable Long id, RedirectAttributes ra) {
    service.archivar(id);
    ra.addFlashAttribute("ok", "archivado");
    return "redirect:/clientes";
  }

  @PostMapping("/{id}/restaurar")
  @PreAuthorize("hasRole('ADMIN')")
  public String restaurar(@PathVariable Long id, RedirectAttributes ra) {
    service.restaurar(id);
    ra.addFlashAttribute("ok", "restaurado");
    ra.addAttribute("estado", "archivados");
    return "redirect:/clientes";
  }

  // actualizar nombre y teléfono (para boton "Modificar") 
  @PostMapping("/{id}/actualizar")
  @PreAuthorize("hasRole('ADMIN')")
  public String actualizar(@PathVariable Long id,
                           @RequestParam @NotBlank String nombre,
                           @RequestParam(required = false) String telefono,
                           // para volver a la misma vista/filtros/pagina:
                           @RequestParam(value = "estado", required = false) String estado,
                           @RequestParam(value = "q", required = false) String q,
                           @RequestParam(value = "page", required = false) Integer page,
                           @RequestParam(value = "size", required = false) Integer size,
                           RedirectAttributes ra) {
    try {
      service.actualizar(id, nombre, telefono);
      ra.addFlashAttribute("ok", "actualizado");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("err", ex.getMessage());
    } catch (DataIntegrityViolationException ex) {
      ra.addFlashAttribute("err", "No se pudo actualizar: conflicto de nombre/teléfono.");
    }
    // conservar filtros y paginacion
    if (estado != null) ra.addAttribute("estado", estado);
    if (q != null && !q.isBlank()) ra.addAttribute("q", q);
    if (page != null) ra.addAttribute("page", page);
    if (size != null) ra.addAttribute("size", size);

    return "redirect:/clientes";
  }

  @GetMapping("/{id}/editar")
@PreAuthorize("hasRole('ADMIN')")
public String editar(@PathVariable Long id,
                     @RequestParam(value = "estado", required = false) String estado,
                     @RequestParam(value = "q", required = false) String q,
                     @RequestParam(value = "page", required = false) Integer page,
                     @RequestParam(value = "size", required = false) Integer size,
                     Model model) {
  var cliente = service.getById(id);
  model.addAttribute("cliente", cliente);

  // para volver al mismo contexto luego de actualizar o cancelar
  model.addAttribute("estado", (estado == null || estado.isBlank()) ? "activos" : estado);
  model.addAttribute("q", q);
  model.addAttribute("page", page);
  model.addAttribute("size", size);

  return "clientes/editar";
}

}
