package com.softfruver.inventario.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        // usa templates/login.html (ya existe)
        return "login";
    }

    // Raíz -> redirige al menú (el handler real de /menu está en MenuController)
    @GetMapping("/")
    public String root() {
        return "redirect:/menu";
    }
}
