package com.softfruver.inventario.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.SecureRandom;
import java.util.UUID;

import static com.softfruver.inventario.security.CaptchaConstants.CAPTCHA_ANSWER_SESSION_KEY;
import static com.softfruver.inventario.security.CaptchaConstants.CAPTCHA_NONCE_SESSION_KEY;
import static com.softfruver.inventario.security.CaptchaConstants.CAPTCHA_QUESTION_SESSION_KEY;

@Controller
public class AuthController {

    private final SecureRandom rnd = new SecureRandom();

    @GetMapping("/login")
    public String login(Model model, HttpSession session, HttpServletResponse response) {
        // No cachear la página de login
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        // Consumir flash de sesión (una sola vez)
        Object flash = session.getAttribute("FLASH_LOGIN_MESSAGE");
        if (flash != null) {
            String key = flash.toString();
            if ("error".equals(key) || "captcha".equals(key) || "expired".equals(key) || "logout".equals(key)) {
                model.addAttribute(key, true);
            }
            session.removeAttribute("FLASH_LOGIN_MESSAGE");
        }

        // Generar captcha + nonce
        int a = 2 + rnd.nextInt(8); // 2..9
        int b = 2 + rnd.nextInt(8); // 2..9
        String pregunta = a + " + " + b + " = ?";

        session.setAttribute(CAPTCHA_ANSWER_SESSION_KEY, String.valueOf(a + b));
        session.setAttribute(CAPTCHA_QUESTION_SESSION_KEY, pregunta);
        session.setAttribute(CAPTCHA_NONCE_SESSION_KEY, UUID.randomUUID().toString());

        model.addAttribute("captchaPregunta", pregunta);
        model.addAttribute("captchaNonce", session.getAttribute(CAPTCHA_NONCE_SESSION_KEY));
        return "login";
    }
}
