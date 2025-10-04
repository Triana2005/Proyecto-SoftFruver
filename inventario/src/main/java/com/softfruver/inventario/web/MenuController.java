package com.softfruver.inventario.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MenuController {

    @GetMapping("/menu")
    public String menu(Model model, Authentication auth) {
        // Puedes pasar datos al menú si quieres
        model.addAttribute("usuario", auth != null ? auth.getName() : "Invitado");
        return "menu"; // templates/menu.html
    }
}
