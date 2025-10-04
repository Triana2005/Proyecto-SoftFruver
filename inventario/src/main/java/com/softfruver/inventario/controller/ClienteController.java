package com.softfruver.inventario.controller;

import com.softfruver.inventario.service.ClienteService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
  public String listado(@RequestParam(value = "q", required = false) String q, Model model) {
    model.addAttribute("q", q);
    model.addAttribute("clientes", (q == null || q.isBlank()) ? service.listar() : service.buscar(q));
    return "clientes/lista";
  }

  @PostMapping
  public String crear(
      @RequestParam @NotBlank String nombre,
      @RequestParam(required = false) String telefono
  ) {
    service.crear(nombre, telefono);
    return "redirect:/clientes?ok=creado";
  }

  @PostMapping("/{id}/archivar")
  public String archivar(@PathVariable Long id) {
    service.archivar(id);
    return "redirect:/clientes?ok=archivado";
  }
}
