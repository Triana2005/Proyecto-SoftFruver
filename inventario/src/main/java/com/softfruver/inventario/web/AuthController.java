package com.softfruver.inventario.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.SecureRandom;

import static com.softfruver.inventario.security.CaptchaConstants.CAPTCHA_ANSWER_SESSION_KEY;

@Controller
public class AuthController {

    private final java.security.SecureRandom rnd = new SecureRandom();

    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        // Genera un captcha aritmético simple (2..9) + (2..9)
        int a = 2 + rnd.nextInt(8);
        int b = 2 + rnd.nextInt(8);
        session.setAttribute(CAPTCHA_ANSWER_SESSION_KEY, a + b);
        model.addAttribute("captchaPregunta", a + " + " + b + " = ?");
        return "login"; // templates/login.html
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/menu";
    }
}
