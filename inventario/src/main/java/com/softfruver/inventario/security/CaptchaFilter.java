package com.softfruver.inventario.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.softfruver.inventario.security.CaptchaConstants.CAPTCHA_ANSWER_SESSION_KEY;

public class CaptchaFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Aplica solo al POST /login
        if ("POST".equalsIgnoreCase(request.getMethod())
                && "/login".equals(request.getServletPath())) {

            String captchaStr = request.getParameter("captcha");
            HttpSession session = request.getSession(false);
            Object expected = (session == null) ? null
                    : session.getAttribute(CAPTCHA_ANSWER_SESSION_KEY);

            boolean ok = false;
            if (captchaStr != null && expected instanceof Integer) {
                try {
                    ok = Integer.parseInt(captchaStr.trim()) == (Integer) expected;
                } catch (NumberFormatException ignored) { }
            }

            if (!ok) {
                response.sendRedirect(request.getContextPath() + "/login?captcha");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
