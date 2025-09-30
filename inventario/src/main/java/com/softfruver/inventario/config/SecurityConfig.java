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
      // CSRF habilitado por defecto (mejor seguridad con formularios Thymeleaf)
      .authorizeHttpRequests(auth -> auth
          // Permitir login y recursos estáticos de tu UI
          .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
          // (Si usas h2-console en dev, descomenta)
          // .requestMatchers("/h2-console/**").permitAll()
          // Todo lo demás requiere autenticación
          .anyRequest().authenticated()
      )
      .formLogin(form -> form
          .loginPage("/login")                 // vista login.html
          .loginProcessingUrl("/login")        // POST del form
          .defaultSuccessUrl("/", true)        // adonde redirige al entrar
          .failureUrl("/login?error")          // en error
          .permitAll()
      )
      .logout(logout -> logout
          .logoutUrl("/logout")
          .logoutSuccessUrl("/login?logout")
          .permitAll()
      )
      // Cabeceras recomendadas
      .headers(h -> h
          // Si usas h2-console, descomenta:
          // .frameOptions(frame -> frame.sameOrigin())
          .contentSecurityPolicy(csp -> csp.policyDirectives(
              // Permite Tailwind CDN en scripts; estilos inline para clases Tailwind generadas.
              "default-src 'self'; " +
              "script-src 'self' https://cdn.tailwindcss.com; " +
              "style-src 'self' 'unsafe-inline'; " +
              "img-src 'self' data:; " +
              "font-src 'self' data:; " +
              "connect-src 'self'; " +
              "frame-ancestors 'self'"
          ))
      )
      .sessionManagement(sm -> sm
          .sessionFixation().migrateSession() // seguro por defecto tras login
      );

    // Si usas h2-console en dev, descomenta estas dos líneas:
    // http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
    // http.headers(h -> h.frameOptions(frame -> frame.sameOrigin()));

    return http.build();
  }
}
