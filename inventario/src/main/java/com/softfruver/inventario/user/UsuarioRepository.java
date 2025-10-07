package com.softfruver.inventario.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Usado en login
    Optional<Usuario> findByUsernameAndActivoTrue(String username);

    // Utilidades para configuración/validaciones
    Optional<Usuario> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<Usuario> findAllByOrderByUsernameAsc();

    List<Usuario> findByRolOrderByUsernameAsc(RolUsuario rol);
}
