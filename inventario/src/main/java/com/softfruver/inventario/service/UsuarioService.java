package com.softfruver.inventario.service;

import com.softfruver.inventario.user.RolUsuario;
import com.softfruver.inventario.user.Usuario;
import com.softfruver.inventario.user.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class UsuarioService {

  private final UsuarioRepository repo;
  private final PasswordEncoder passwordEncoder;

  public UsuarioService(UsuarioRepository repo, PasswordEncoder passwordEncoder) {
    this.repo = repo;
    this.passwordEncoder = passwordEncoder;
  }

  // ====== Utilidades ======
  public Usuario getById(Long id) {
    return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
  }

  // ====== Crear secretaria ======
  public void crearSecretaria(String username, String rawPassword) {
    if (username == null || username.isBlank()) throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
    if (rawPassword == null || rawPassword.isBlank()) throw new IllegalArgumentException("La contraseña es obligatoria.");

    String u = username.trim();
    if (u.length() < 3) throw new IllegalArgumentException("El usuario debe tener al menos 3 caracteres.");
    if (rawPassword.length() < 6) throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
    if (repo.existsByUsernameIgnoreCase(u)) throw new IllegalArgumentException("El nombre de usuario ya existe.");

    Usuario nuevo = new Usuario();
    nuevo.setUsername(u);
    nuevo.setPassHash(passwordEncoder.encode(rawPassword));
    nuevo.setRol(RolUsuario.SECRETARIA);
    nuevo.setActivo(true);
    nuevo.setCreadoEn(OffsetDateTime.now());
    nuevo.setActualizadoEn(OffsetDateTime.now());
    repo.save(nuevo);
  }

  // ====== Listados ======
  public List<Usuario> listarTodos() {
    return repo.findAllByOrderByUsernameAsc();
  }

  // ====== Cambiar contraseña (otro usuario) ======
  public void cambiarPasswordDe(Long idUsuario, String nuevaPassword) {
    if (nuevaPassword == null || nuevaPassword.isBlank())
      throw new IllegalArgumentException("La nueva contraseña es obligatoria.");
    if (nuevaPassword.length() < 6)
      throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");

    Usuario u = getById(idUsuario);
    u.setPassHash(passwordEncoder.encode(nuevaPassword));
    u.setActualizadoEn(OffsetDateTime.now());
    repo.save(u);
  }

  // ====== Cambiar mi contraseña (admin actual) ======
  public void cambiarMiPassword(String nuevaPassword) {
    if (nuevaPassword == null || nuevaPassword.isBlank())
      throw new IllegalArgumentException("La nueva contraseña es obligatoria.");
    if (nuevaPassword.length() < 6)
      throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null)
      throw new IllegalStateException("No hay sesión activa.");

    Usuario yo = repo.findByUsernameIgnoreCase(auth.getName())
        .orElseThrow(() -> new IllegalStateException("Usuario actual no encontrado."));

    yo.setPassHash(passwordEncoder.encode(nuevaPassword));
    yo.setActualizadoEn(OffsetDateTime.now());
    repo.save(yo);
  }

  // ====== Activar / Desactivar ======
  public void setActivo(Long idUsuario, boolean activo) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String current = (auth != null) ? auth.getName() : null;

    Usuario u = getById(idUsuario);

    if (u.getRol() == RolUsuario.ADMIN)
      throw new IllegalArgumentException("No se puede cambiar el estado del ADMIN.");

    if (current != null && u.getUsername().equalsIgnoreCase(current))
      throw new IllegalArgumentException("No puedes cambiar el estado de tu propia cuenta.");

    u.setActivo(activo);
    u.setActualizadoEn(OffsetDateTime.now());
    repo.save(u);
  }

  // ====== Actualizar username y/o password (opcionales, funciona aunque esté inactivo) ======
  public void actualizarUsuario(Long idUsuario, String nuevoUsername, String nuevaPassword) {
    boolean hayUsername = nuevoUsername != null && !nuevoUsername.isBlank();
    boolean hayPassword = nuevaPassword != null && !nuevaPassword.isBlank();

    if (!hayUsername && !hayPassword)
      throw new IllegalArgumentException("Debes proporcionar nuevo usuario y/o nueva contraseña.");

    Usuario u = getById(idUsuario);

    if (hayUsername) {
      String u2 = nuevoUsername.trim();
      if (u2.length() < 3) throw new IllegalArgumentException("El usuario debe tener al menos 3 caracteres.");
      if (!u.getUsername().equalsIgnoreCase(u2) && repo.existsByUsernameIgnoreCase(u2))
        throw new IllegalArgumentException("El nombre de usuario ya existe.");
      u.setUsername(u2);
    }

    if (hayPassword) {
      if (nuevaPassword.length() < 6) throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
      u.setPassHash(passwordEncoder.encode(nuevaPassword));
    }

    u.setActualizadoEn(OffsetDateTime.now());
    repo.save(u);
  }

  // ====== Borrado duro (solo SECRETARIA) ======
  public void borrarFisicamenteSecretaria(Long idUsuario) {
    Usuario u = getById(idUsuario);
    if (u.getRol() == RolUsuario.ADMIN)
      throw new IllegalArgumentException("No se puede borrar al ADMIN.");
    repo.delete(u);
  }
}
