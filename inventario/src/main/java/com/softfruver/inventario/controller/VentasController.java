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

import com.softfruver.inventario.repository.VentasRepository;
import com.softfruver.inventario.service.VentasService;

@Controller
@RequestMapping("/ventas")
public class VentasController {

    private final VentasRepository ventasRepository;
    private final VentasService ventasService;

    public VentasController(VentasRepository ventasRepository, VentasService ventasService) {
        this.ventasRepository = ventasRepository;
        this.ventasService = ventasService;
    }

    // ---------- Lista ----------
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String listaVentas(
            @RequestParam(name = "q", required = false) String q, // nombre del cliente
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        String cli = (q == null || q.isBlank()) ? null : q.trim();

        List<VentasRepository.VentaItemListado> items = ventasRepository.buscarVentas(desde, hasta, cli);

        model.addAttribute("items", items);
        model.addAttribute("q", q);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        // Para el datalist del filtro Cliente
        model.addAttribute("clientes", ventasRepository.opcionesClientes());

        return "ventas/lista";
    }

    // ---------- Nueva venta (form) ----------
    @GetMapping("/nueva")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String nuevaVenta(Model model) {
        model.addAttribute("clientes", ventasRepository.opcionesClientes());
        model.addAttribute("productos", ventasRepository.opcionesProductos());
        model.addAttribute("hoy", LocalDate.now());
        return "ventas/form";
    }

    // ---------- Guardar venta ----------
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String guardarVenta(
            @RequestParam("clienteId") Long clienteId,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "credito", required = false, defaultValue = "false") boolean esCredito,

            @RequestParam(value = "productoId", required = false) List<Long> productoIds,
            @RequestParam(value = "cantidadKg", required = false) List<BigDecimal> cantidades,
            @RequestParam(value = "precioUnit", required = false) List<BigDecimal> precios,

            Model model) {
        try {
            List<VentasService.ItemNuevaVenta> items = new ArrayList<>();
            if (productoIds != null) {
                for (int i = 0; i < productoIds.size(); i++) {
                    Long pid = productoIds.get(i);
                    BigDecimal cant = (cantidades != null && i < cantidades.size()) ? cantidades.get(i) : null;
                    BigDecimal prec = (precios != null && i < precios.size()) ? precios.get(i) : null;
                    items.add(new VentasService.ItemNuevaVenta(pid, cant, prec));
                }
            }

            Long ventaId = ventasService.registrarVenta(clienteId, fecha, esCredito, items);
            return "redirect:/ventas/" + ventaId;

        } catch (Exception ex) {
            // Re-cargar combos y devolver el form con error
            model.addAttribute("clientes", ventasRepository.opcionesClientes());
            model.addAttribute("productos", ventasRepository.opcionesProductos());
            model.addAttribute("hoy", fecha != null ? fecha : LocalDate.now());
            model.addAttribute("error", ex.getMessage());
            return "ventas/form";
        }
    }

    // ---------- Detalle ----------
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String verVenta(@PathVariable("id") Long id, Model model) {
        var cab = ventasRepository.obtenerCabecera(id);
        if (cab == null) {
            model.addAttribute("items", java.util.List.of());
            model.addAttribute("q", null);
            model.addAttribute("desde", null);
            model.addAttribute("hasta", null);
            model.addAttribute("error", "La venta " + id + " no existe.");
            return "ventas/lista";
        }

        var detalle = ventasRepository.obtenerDetalle(id);

        model.addAttribute("cab", cab);
        model.addAttribute("ventaId", id);
        model.addAttribute("detalle", detalle);
        return "ventas/detalle";
    }

    // ---------- Opciones (si las usas vía Ajax) ----------
    @GetMapping("/opciones/clientes")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public List<VentasRepository.Opcion> opcionesClientes() {
        return ventasRepository.opcionesClientes();
    }

    @GetMapping("/opciones/productos")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public List<VentasRepository.Opcion> opcionesProductos() {
        return ventasRepository.opcionesProductos();
    }

    // EDITAR (form)
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String editarVenta(@PathVariable Long id, Model model) {
        var cab = ventasRepository.obtenerCabecera(id);
        if (cab == null) {
            model.addAttribute("error", "La venta " + id + " no existe.");
            model.addAttribute("items", java.util.List.of());
            return "ventas/lista";
        }
        var detalle = ventasRepository.obtenerDetalle(id);

        model.addAttribute("cab", cab);
        model.addAttribute("ventaId", id);
        model.addAttribute("detalle", detalle);

        // combos
        model.addAttribute("clientes", ventasRepository.opcionesClientes());
        model.addAttribute("productos", ventasRepository.opcionesProductos());

        return "ventas/form-editar"; // crea esta vista (puedes reutilizar el form con valores precargados)
    }

    // ACTUALIZAR (submit)
    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String actualizarVenta(
            @PathVariable Long id,
            @RequestParam("clienteId") Long clienteId,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "credito", required = false, defaultValue = "false") boolean esCredito,

            @RequestParam(value = "productoId", required = false) List<Long> productoIds,
            @RequestParam(value = "cantidadKg", required = false) List<BigDecimal> cantidades,
            @RequestParam(value = "precioUnit", required = false) List<BigDecimal> precios,

            Model model) {
        try {
            List<VentasService.ItemNuevaVenta> items = new ArrayList<>();
            if (productoIds != null) {
                for (int i = 0; i < productoIds.size(); i++) {
                    Long pid = productoIds.get(i);
                    BigDecimal cant = (cantidades != null && i < cantidades.size()) ? cantidades.get(i) : null;
                    BigDecimal prec = (precios != null && i < precios.size()) ? precios.get(i) : null;
                    items.add(new VentasService.ItemNuevaVenta(pid, cant, prec));
                }
            }
            ventasService.modificarVenta(id, clienteId, fecha, esCredito, items);
            return "redirect:/ventas/" + id;

        } catch (Exception ex) {
            // recargar combos y volver al form de edición con error
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("clientes", ventasRepository.opcionesClientes());
            model.addAttribute("productos", ventasRepository.opcionesProductos());

            var cab = ventasRepository.obtenerCabecera(id);
            var detalle = ventasRepository.obtenerDetalle(id);
            model.addAttribute("cab", cab);
            model.addAttribute("ventaId", id);
            model.addAttribute("detalle", detalle);
            return "ventas/form-editar";
        }
    }

    // ELIMINAR
    @PostMapping("/{id}/eliminar")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    public String eliminarVenta(@PathVariable Long id, Model model) {
        try {
            ventasService.eliminarVenta(id);
            return "redirect:/ventas";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            return "redirect:/ventas/" + id;
        }
    }

}
