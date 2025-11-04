package com.softfruver.inventario.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.softfruver.inventario.repository.ComprasRepository;
import com.softfruver.inventario.service.ComprasService;

@Controller
@RequestMapping("/compras")
public class ComprasController {

  private final ComprasRepository comprasRepository;
  private final ComprasService comprasService;

  public ComprasController(ComprasRepository comprasRepository, ComprasService comprasService) {
    this.comprasRepository = comprasRepository;
    this.comprasService = comprasService;
  }

  // ---------- Lista ----------
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
  public String listaCompras(
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
      Model model) {
    String prov = (q == null || q.isBlank()) ? null : q.trim();

    List<ComprasRepository.CompraItem> items = comprasRepository.buscarCompras(desde, hasta, prov);

    model.addAttribute("items", items);
    model.addAttribute("q", q);
    model.addAttribute("desde", desde);
    model.addAttribute("hasta", hasta);

    // datalist del filtro Proveedor
    model.addAttribute("proveedores", comprasRepository.opcionesProveedores());

    return "compras/lista";
  }

  // ---------- Nueva compra (form) ----------
  @GetMapping("/nueva")
  @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
  public String nuevaCompra(Model model) {
    model.addAttribute("proveedores", comprasRepository.opcionesProveedores());
    model.addAttribute("productos", comprasRepository.opcionesProductos());
    model.addAttribute("hoy", LocalDate.now());
    return "compras/form";
  }

  // ---------- Guardar compra ----------
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
  public String guardarCompra(
      @RequestParam("proveedorId") Long proveedorId,
      @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,

      @RequestParam(value = "productoId", required = false) List<Long> productoIds,
      @RequestParam(value = "cantidadKg", required = false) List<BigDecimal> cantidades,
      @RequestParam(value = "precioUnit", required = false) List<BigDecimal> precios,

      Model model) {
    try {
      List<ComprasService.ItemNuevaCompra> items = new ArrayList<>();
      if (productoIds != null) {
        for (int i = 0; i < productoIds.size(); i++) {
          Long pid = productoIds.get(i);
          BigDecimal cant = (cantidades != null && i < cantidades.size()) ? cantidades.get(i) : null;
          BigDecimal prec = (precios != null && i < precios.size()) ? precios.get(i) : null;
          items.add(new ComprasService.ItemNuevaCompra(pid, cant, prec));
        }
      }

      Long compraId = comprasService.registrarCompra(proveedorId, fecha, items);
      return "redirect:/compras/" + compraId;

    } catch (Exception ex) {
      model.addAttribute("proveedores", comprasRepository.opcionesProveedores());
      model.addAttribute("productos", comprasRepository.opcionesProductos());
      model.addAttribute("hoy", fecha != null ? fecha : LocalDate.now());
      model.addAttribute("error", ex.getMessage());
      return "compras/form";
    }
  }

  // ---------- Detalle ----------
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
  public String verCompra(@PathVariable("id") Long id, Model model) {
    var cab = comprasRepository.obtenerCabecera(id);
    if (cab == null) {
      model.addAttribute("items", java.util.List.of());
      model.addAttribute("q", null);
      model.addAttribute("desde", null);
      model.addAttribute("hasta", null);
      model.addAttribute("error", "La compra " + id + " no existe.");
      return "compras/lista";
    }

    var detalle = comprasRepository.obtenerDetalle(id);

    model.addAttribute("cab", cab);
    model.addAttribute("compraId", id);
    model.addAttribute("detalle", detalle);
    return "compras/detalle";
  }

  // ---------- Opciones (si las usas vía Ajax) ----------
  @GetMapping("/opciones/proveedores")
  @ResponseBody
  @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
  public List<ComprasRepository.Opcion> opcionesProveedores() {
    return comprasRepository.opcionesProveedores();
  }

  @GetMapping("/opciones/productos")
  @ResponseBody
  @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
  public List<ComprasRepository.Opcion> opcionesProductos() {
    return comprasRepository.opcionesProductos();
  }

  // ---------- EDITAR (form) ----------
  @GetMapping("/{id}/editar")
  @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
  public String editarCompra(@PathVariable Long id, Model model) {
    var cab = comprasRepository.obtenerCabecera(id);
    if (cab == null) {
      model.addAttribute("error", "La compra " + id + " no existe.");
      model.addAttribute("items", java.util.List.of());
      return "compras/lista";
    }
    var detalle = comprasRepository.obtenerDetalle(id);

    model.addAttribute("cab", cab);          // tiene proveedorId expuesto (ver repo)
    model.addAttribute("compraId", id);
    model.addAttribute("detalle", detalle);

    // combos
    model.addAttribute("proveedores", comprasRepository.opcionesProveedores());
    model.addAttribute("productos", comprasRepository.opcionesProductos());

    return "compras/form-editar";
  }

  // ---------- ACTUALIZAR (submit) ----------
  @PostMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
  public String actualizarCompra(
      @PathVariable Long id,
      @RequestParam("proveedorId") Long proveedorId,
      @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,

      @RequestParam(value = "productoId", required = false) List<Long> productoIds,
      @RequestParam(value = "cantidadKg", required = false) List<BigDecimal> cantidades,
      @RequestParam(value = "precioUnit", required = false) List<BigDecimal> precios,

      Model model) {
    try {
      List<ComprasService.ItemNuevaCompra> items = new ArrayList<>();
      if (productoIds != null) {
        for (int i = 0; i < productoIds.size(); i++) {
          Long pid = productoIds.get(i);
          BigDecimal cant = (cantidades != null && i < cantidades.size()) ? cantidades.get(i) : null;
          BigDecimal prec = (precios != null && i < precios.size()) ? precios.get(i) : null;
          items.add(new ComprasService.ItemNuevaCompra(pid, cant, prec));
        }
      }

      comprasService.modificarCompra(id, proveedorId, fecha, items);
      return "redirect:/compras/" + id;

    } catch (Exception ex) {
      model.addAttribute("error", ex.getMessage());
      model.addAttribute("proveedores", comprasRepository.opcionesProveedores());
      model.addAttribute("productos", comprasRepository.opcionesProductos());

      var cab = comprasRepository.obtenerCabecera(id);
      var detalle = comprasRepository.obtenerDetalle(id);
      model.addAttribute("cab", cab);
      model.addAttribute("compraId", id);
      model.addAttribute("detalle", detalle);
      return "compras/form-editar";
    }
  }

  // ---------- ELIMINAR ----------
  @PostMapping("/{id}/eliminar")
  @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
  public String eliminarCompra(@PathVariable Long id, Model model) {
    try {
      comprasService.eliminarCompra(id);
      return "redirect:/compras";
    } catch (Exception ex) {
      model.addAttribute("error", ex.getMessage());
      return "redirect:/compras/" + id;
    }
  }
}
