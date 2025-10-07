package com.softfruver.inventario.controller;

import com.softfruver.inventario.service.ClienteService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;



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
  Page<com.softfruver.inventario.repository.projection.ClienteListado> page =
      service.buscarPorEstado(verArchivados, q, pageable);

  model.addAttribute("estado", verArchivados ? "archivados" : "activos");
  model.addAttribute("q", q);
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
      ra.addFlashAttribute("ok", "creado");          // Flash para mensaje
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("err", ex.getMessage());  // Flash error
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
    ra.addFlashAttribute("ok", "archivado");         // Flash para mensaje
    return "redirect:/clientes";
  }

  @PostMapping("/{id}/restaurar")
  @PreAuthorize("hasRole('ADMIN')")
  public String restaurar(@PathVariable Long id, RedirectAttributes ra) {
    service.restaurar(id);
    ra.addFlashAttribute("ok", "restaurado");        // Flash para mensaje
    ra.addAttribute("estado", "archivados");         // Param navegable (filtro)
    return "redirect:/clientes";
  }
}


