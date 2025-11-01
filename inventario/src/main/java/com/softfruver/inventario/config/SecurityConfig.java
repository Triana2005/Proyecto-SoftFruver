package com.softfruver.inventario.config;

import com.softfruver.inventario.security.CaptchaFilter;
import com.softfruver.inventario.security.LoginFailureHandler;
import com.softfruver.inventario.security.LogoutSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public CaptchaFilter captchaFilter() {
    return new CaptchaFilter();
  }

  @Bean
  public LoginFailureHandler loginFailureHandler() {
    return new LoginFailureHandler();
  }

  @Bean
  public LogoutSuccessHandler logoutSuccessHandler() {
    return new LogoutSuccessHandler();
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/login", "/login/",
                "/favicon.ico",
                "/css/**", "/js/**",
                "/images/**", "/imagenes/**",
                "/iconos/**",
                "/webjars/**"
            ).permitAll()
            .requestMatchers("/clientes/**").hasAnyRole("ADMIN", "SECRETARIA")
            .anyRequest().authenticated())

        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .defaultSuccessUrl("/menu", true)
            .failureHandler(loginFailureHandler())   // <- sin ?error
            .permitAll())

        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler(logoutSuccessHandler()) // <- sin ?logout
            .permitAll())

        .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; " +
                "script-src 'self' https://cdn.tailwindcss.com; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data:; " +
                "font-src 'self' data:; " +
                "connect-src 'self'; " +
                "frame-ancestors 'self'")))
        .sessionManagement(sm -> sm.sessionFixation().migrateSession());

    // Captcha ANTES del filtro de auth
    http.addFilterBefore(captchaFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
