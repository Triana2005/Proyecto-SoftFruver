// src/main/java/com/softfruver/inventario/web/MenuController.java
package com.softfruver.inventario.web;

import com.softfruver.inventario.repository.DashboardRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.*;

@Controller
public class MenuController {

    private final DashboardRepository dashboard;

    public MenuController(DashboardRepository dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping({"/", "/menu"})
    public String menu(Model model, Authentication auth) {

        model.addAttribute("usuario", auth != null ? auth.getName() : "Invitado");

        // Día local Cúcuta/Bogotá
        ZoneId ZONA = ZoneId.of("America/Bogota");
        LocalDate hoy = LocalDate.now(ZONA);
        Instant ini = hoy.atStartOfDay(ZONA).toInstant();
        Instant fin = hoy.plusDays(1).atStartOfDay(ZONA).toInstant();

        long ventasCount   = dashboard.contarVentas(ini, fin);
        long comprasCount  = dashboard.contarCompras(ini, fin);
        long pagosCount    = dashboard.contarPagos(ini, fin);

        BigDecimal ventasTotal  = dashboard.totalVentas(ini, fin);
        BigDecimal comprasTotal = dashboard.totalCompras(ini, fin);
        BigDecimal pagosTotal   = dashboard.totalPagos(ini, fin);

        model.addAttribute("hoy", hoy);
        model.addAttribute("ventasCount", ventasCount);
        model.addAttribute("comprasCount", comprasCount);
        model.addAttribute("pagosCount", pagosCount);
        model.addAttribute("ventasTotal", ventasTotal);
        model.addAttribute("comprasTotal", comprasTotal);
        model.addAttribute("pagosTotal", pagosTotal);

        return "menu"; // templates/menu.html
    }
}
