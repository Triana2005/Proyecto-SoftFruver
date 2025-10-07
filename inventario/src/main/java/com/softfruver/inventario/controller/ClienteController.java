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
      Model model
  ) {
    boolean verArchivados = "archivados".equalsIgnoreCase(estado);
    model.addAttribute("estado", verArchivados ? "archivados" : "activos");
    model.addAttribute("q", q);
    model.addAttribute("clientes", service.buscarPorEstado(verArchivados, q));
    return "clientes/lista";
  }

  @PostMapping
  public String crear(
      @RequestParam @NotBlank String nombre,
      @RequestParam(required = false) String telefono,
      RedirectAttributes ra
  ) {
    try {
      service.crear(nombre, telefono);
      ra.addAttribute("ok", "creado");
    } catch (IllegalArgumentException ex) {
      // Mensaje del Service (nombre vacío o duplicado normalizado)
      ra.addAttribute("err", ex.getMessage());
    } catch (DataIntegrityViolationException ex) {
      // Respaldo por si el índice único de BD salta primero
      ra.addAttribute("err", "Ya existe un cliente activo con ese nombre (ignorando tildes y mayúsculas).");
    }
    return "redirect:/clientes";
  }

  @PostMapping("/{id}/archivar")
  public String archivar(@PathVariable Long id, RedirectAttributes ra) {
    service.archivar(id);
    ra.addAttribute("ok", "archivado");
    return "redirect:/clientes";
  }

  @PostMapping("/{id}/restaurar")
  public String restaurar(@PathVariable Long id, RedirectAttributes ra) {
    service.restaurar(id);
    ra.addAttribute("ok", "restaurado");
    ra.addAttribute("estado", "archivados");
    return "redirect:/clientes";
  }
}

