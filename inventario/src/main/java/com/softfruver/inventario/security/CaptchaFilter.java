package com.softfruver.inventario.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

import static com.softfruver.inventario.security.CaptchaConstants.CAPTCHA_ANSWER_SESSION_KEY;
import static com.softfruver.inventario.security.CaptchaConstants.CAPTCHA_NONCE_SESSION_KEY;
import static com.softfruver.inventario.security.CaptchaConstants.CAPTCHA_QUESTION_SESSION_KEY;

public class CaptchaFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CaptchaFilter.class);

    private void clearCaptcha(HttpSession session) {
        if (session != null) {
            session.removeAttribute(CAPTCHA_ANSWER_SESSION_KEY);
            session.removeAttribute(CAPTCHA_QUESTION_SESSION_KEY);
            session.removeAttribute(CAPTCHA_NONCE_SESSION_KEY);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String uri = request.getRequestURI();
        final boolean isLoginPost = "POST".equalsIgnoreCase(request.getMethod())
                && (uri.endsWith("/login") || uri.endsWith("/login/"));

        if (isLoginPost) {
            // (logs útiles si estás depurando proxys/cookies)
            String requestedSid = request.getRequestedSessionId();
            String cookieSid = null;
            if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if ("JSESSIONID".equalsIgnoreCase(c.getName())) {
                        cookieSid = c.getValue();
                        break;
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Login POST diag: uri={}, requestedSid={}, cookieSid={}, hasCookies={}",
                        uri, requestedSid, cookieSid, request.getCookies() != null);
            }

            HttpSession session = request.getSession(false);

            String nonceFromSession = (session != null)
                    ? (String) session.getAttribute(CAPTCHA_NONCE_SESSION_KEY)
                    : null;
            String nonceFromForm = request.getParameter("captchaNonce");

            if (log.isDebugEnabled()) {
                log.debug("Captcha check: uri={}, sessionId={}, nonceSession={}, nonceForm={}",
                        uri, (session != null ? session.getId() : "null"),
                        nonceFromSession, nonceFromForm);
            }

            // Igualdad estricta del NONCE (anti-form reusado)
            if (session == null || nonceFromSession == null || nonceFromForm == null
                    || !Objects.equals(nonceFromSession, nonceFromForm)) {
                if (session != null) session.setAttribute("FLASH_LOGIN_MESSAGE", "expired");
                clearCaptcha(session);
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }

            // Validación aritmética robusta (String/Integer/Long)
            Object expectedObj = session.getAttribute(CAPTCHA_ANSWER_SESSION_KEY);
            String provided = request.getParameter("captcha");

            boolean ok = false;
            try {
                String expectedStr = (expectedObj == null) ? null : expectedObj.toString();
                if (expectedStr != null && provided != null) {
                    int expectedInt = Integer.parseInt(expectedStr.trim());
                    int providedInt = Integer.parseInt(provided.trim());
                    ok = (expectedInt == providedInt);
                    if (log.isDebugEnabled()) {
                        log.debug("Captcha values: expected={}, provided={}, result={}",
                                expectedInt, providedInt, ok);
                    }
                }
            } catch (NumberFormatException nfe) {
                if (log.isDebugEnabled()) {
                    log.debug("Captcha parse error. expectedObj={}, provided='{}'", expectedObj, provided);
                }
            }

            if (!ok) {
                if (session != null) session.setAttribute("FLASH_LOGIN_MESSAGE", "captcha");
                clearCaptcha(session);
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }

            // Éxito: invalida captcha para evitar reuso
            clearCaptcha(session);
        }

        filterChain.doFilter(request, response);
    }
}
