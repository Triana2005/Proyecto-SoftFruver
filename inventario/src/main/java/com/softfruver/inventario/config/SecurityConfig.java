package com.softfruver.inventario.config;

import com.softfruver.inventario.security.CaptchaFilter;
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
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/login",
                "/css/**",
                "/js/**",
                "/images/**",
                "/imagenes/**",   
                "/webjars/**"
            ).permitAll()
            .requestMatchers("/clientes/**").hasAnyRole("ADMIN", "SECRETARIA")
            // añade otros módulos con sus reglas si lo necesito
            .anyRequest().authenticated())

        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .defaultSuccessUrl("/menu", true)
            .failureUrl("/login?error")
            .permitAll())

        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
            .permitAll())

        .headers(h -> h
            .contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; " +
                "script-src 'self' https://cdn.tailwindcss.com; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data:; " +
                "font-src 'self' data:; " +
                "connect-src 'self'; " +
                "frame-ancestors 'self'")))
        .sessionManagement(sm -> sm.sessionFixation().migrateSession());

    // inserta el filtro de captcha ANTES del de autenticacion de usuario/clave
    http.addFilterBefore(captchaFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
