package com.softfruver.inventario.controller;

import com.softfruver.inventario.repository.InventarioRepository;
import com.softfruver.inventario.repository.projection.InventarioItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioRepository inventarioRepository;

    @GetMapping("/inventario")
    public String lista(@RequestParam(name = "q", required = false) String q, Model model) {
        final String criterio = (q == null) ? "" : q.trim();

        List<InventarioItem> items = criterio.isEmpty()
                ? inventarioRepository.listar()
                : inventarioRepository.buscar(criterio);

        model.addAttribute("items", items);
        model.addAttribute("totalProductos", inventarioRepository.totalProductos());
        model.addAttribute("totalAlertas", inventarioRepository.totalAlertas());
        model.addAttribute("q", criterio);

        return "inventario/lista";
    }
}
