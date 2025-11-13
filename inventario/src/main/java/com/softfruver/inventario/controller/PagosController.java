package com.softfruver.inventario.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.softfruver.inventario.repository.PagosRepository;
import com.softfruver.inventario.service.PagosService;

@Controller
@RequestMapping("/pagos")
public class PagosController {

    private final PagosRepository pagosRepository;
    private final PagosService pagosService;

    public PagosController(PagosRepository pagosRepository, PagosService pagosService) {
        this.pagosRepository = pagosRepository;
        this.pagosService = pagosService;
    }

    // ---------- Lista ----------
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String listaPagos(
            @RequestParam(name = "q", required = false) String q, // nombre del cliente/proveedor
            @RequestParam(name = "tipo", required = false) String tipo, // CLIENTE | PROVEEDOR | null
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {

        final String filtro = (q == null || q.isBlank()) ? null : q.trim();
        final String tipoNorm = (tipo == null || tipo.isBlank()) ? null : tipo.trim().toUpperCase();

        var items = pagosRepository.buscarPagos(desde, hasta, filtro, tipoNorm);

        model.addAttribute("items", items);
        model.addAttribute("q", q);
        model.addAttribute("tipo", tipoNorm);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        model.addAttribute("clientes", pagosRepository.opcionesClientes());
        model.addAttribute("proveedores", pagosRepository.opcionesProveedores());

        return "pagos/lista";
    }

    // ---------- Nuevo (form) ----------
    @GetMapping("/nuevo")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String nuevoPago(Model model) {
        model.addAttribute("clientes", pagosRepository.opcionesClientes());
        model.addAttribute("proveedores", pagosRepository.opcionesProveedores());
        model.addAttribute("hoy", LocalDate.now());
        return "pagos/form";
    }

    // ---------- Guardar ----------
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String guardarPago(
            @RequestParam("tipo") String tipo, // CLIENTE | PROVEEDOR
            @RequestParam("refId") Long refId, // cliente_id o proveedor_id
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam("monto") BigDecimal monto,
            @RequestParam("metodo") String metodo, // EFECTIVO | TRANSFERENCIA
            Model model) {
        try {
            Long pagoId = pagosService.registrarPago(tipo, refId, fecha, monto, metodo);
            return "redirect:/pagos/" + pagoId;

        } catch (Exception ex) {
            model.addAttribute("clientes", pagosRepository.opcionesClientes());
            model.addAttribute("proveedores", pagosRepository.opcionesProveedores());
            model.addAttribute("hoy", (fecha != null ? fecha : LocalDate.now()));
            model.addAttribute("error", ex.getMessage());
            return "pagos/form";
        }
    }

    // ---------- Detalle ----------
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String verPago(@PathVariable Long id, Model model) {
        var cab = pagosRepository.obtenerCabecera(id);
        if (cab == null) {
            model.addAttribute("items", List.of());
            model.addAttribute("error", "El pago " + id + " no existe.");
            return "pagos/lista";
        }
        model.addAttribute("cab", cab);
        model.addAttribute("pagoId", id);
        return "pagos/detalle";
    }

    // ---------- Editar (form) ----------
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String editarPago(@PathVariable Long id, Model model) {
        var cab = pagosRepository.obtenerCabecera(id);
        if (cab == null) {
            model.addAttribute("error", "El pago " + id + " no existe.");
            model.addAttribute("items", List.of());
            return "pagos/lista";
        }
        model.addAttribute("cab", cab);
        model.addAttribute("pagoId", id);
        model.addAttribute("clientes", pagosRepository.opcionesClientes());
        model.addAttribute("proveedores", pagosRepository.opcionesProveedores());
        return "pagos/form-editar";
    }

    // ---------- Actualizar ----------
    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String actualizarPago(
            @PathVariable Long id,
            @RequestParam("tipo") String tipo,
            @RequestParam("refId") Long refId,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam("monto") BigDecimal monto,
            @RequestParam("metodo") String metodo,
            Model model) {
        try {
            pagosService.modificarPago(id, tipo, refId, fecha, monto, metodo);
            return "redirect:/pagos/" + id;

        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("clientes", pagosRepository.opcionesClientes());
            model.addAttribute("proveedores", pagosRepository.opcionesProveedores());
            var cab = pagosRepository.obtenerCabecera(id);
            model.addAttribute("cab", cab);
            model.addAttribute("pagoId", id);
            return "pagos/form-editar";
        }
    }

    // ---------- Eliminar ----------
    @PostMapping("/{id}/eliminar")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String eliminarPago(@PathVariable Long id, Model model) {
        try {
            pagosService.eliminarPago(id);
            return "redirect:/pagos";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            return "redirect:/pagos/" + id;
        }
    }
}
