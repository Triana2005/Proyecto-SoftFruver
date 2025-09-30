package com.softfruver.inventario.security;

import com.softfruver.inventario.user.Usuario;
import com.softfruver.inventario.user.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repo;

    public UsuarioDetailsService(UsuarioRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = repo.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado o inactivo"));

        // Spring Security requiere el prefijo ROLE_ para las autoridades
        String role = "ROLE_" + u.getRol().name();
        return User.withUsername(u.getUsername())
                .password(u.getPassHash()) // ya viene hasheado en DB (BCrypt)
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
    }
}
