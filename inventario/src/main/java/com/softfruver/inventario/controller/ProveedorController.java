package com.softfruver.inventario.controller;

import com.softfruver.inventario.repository.projection.ProveedorListado;
import com.softfruver.inventario.service.ProveedorService;
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
@RequestMapping("/proveedores")
@Validated
@PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
public class ProveedorController {

  private final ProveedorService service;

  public ProveedorController(ProveedorService service) {
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
    Page<ProveedorListado> page = service.buscarPorEstado(verArchivados, q, pageable);

    model.addAttribute("estado", verArchivados ? "archivados" : "activos");
    model.addAttribute("q", q);
    model.addAttribute("page", page);
    return "proveedores/lista";
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
          "Ya existe un proveedor activo con ese nombre o teléfono.");
    }
    return "redirect:/proveedores";
  }

  @PostMapping("/{id}/archivar")
  @PreAuthorize("hasRole('ADMIN')")
  public String archivar(@PathVariable Long id, RedirectAttributes ra) {
    service.archivar(id);
    ra.addFlashAttribute("ok", "archivado");
    return "redirect:/proveedores";
  }

  @PostMapping("/{id}/restaurar")
  @PreAuthorize("hasRole('ADMIN')")
  public String restaurar(@PathVariable Long id, RedirectAttributes ra) {
    service.restaurar(id);
    ra.addFlashAttribute("ok", "restaurado");
    ra.addAttribute("estado", "archivados");
    return "redirect:/proveedores";
  }

  // Editar / Actualizar 
  @GetMapping("/{id}/editar")
  @PreAuthorize("hasRole('ADMIN')")
  public String editar(@PathVariable Long id,
                       @RequestParam(value = "estado", required = false) String estado,
                       @RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false) Integer size,
                       Model model) {
    var proveedor = service.getById(id);
    model.addAttribute("proveedor", proveedor);
    model.addAttribute("estado", (estado == null || estado.isBlank()) ? "activos" : estado);
    model.addAttribute("q", q);
    model.addAttribute("page", page);
    model.addAttribute("size", size);
    return "proveedores/editar";
  }

  @PostMapping("/{id}/actualizar")
  @PreAuthorize("hasRole('ADMIN')")
  public String actualizar(@PathVariable Long id,
                           @RequestParam String nombre,
                           @RequestParam(required = false) String telefono,
                           @RequestParam(value = "estado", required = false) String estado,
                           @RequestParam(value = "q", required = false) String q,
                           @RequestParam(value = "page", required = false) Integer page,
                           @RequestParam(value = "size", required = false) Integer size,
                           RedirectAttributes ra) {
    try {
      service.actualizar(id, nombre, telefono);
      ra.addFlashAttribute("ok", "modificado");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("err", ex.getMessage());
      ra.addAttribute("estado", estado);
      ra.addAttribute("q", q);
      return "redirect:/proveedores/" + id + "/editar";
    } catch (DataIntegrityViolationException ex) {
      ra.addFlashAttribute("err", "Nombre o teléfono violan una restricción única.");
      ra.addAttribute("estado", estado);
      ra.addAttribute("q", q);
      return "redirect:/proveedores/" + id + "/editar";
    }

    ra.addAttribute("estado", estado);
    ra.addAttribute("q", q);
    ra.addAttribute("page", page);
    ra.addAttribute("size", size);
    return "redirect:/proveedores";
  }
}
