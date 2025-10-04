package com.softfruver.inventario.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
            // Clientes (ADMIN y SECRETARIA)
            .requestMatchers("/clientes/**").hasAnyRole("ADMIN", "SECRETARIA")
            // (cuando crees otros módulos, añade sus reglas arriba)
            .anyRequest().authenticated())

        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .defaultSuccessUrl("/menu", true) // <- aquí al menú directo
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

    return http.build();
  }
}
