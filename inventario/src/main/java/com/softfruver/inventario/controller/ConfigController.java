package com.softfruver.inventario.controller;

import com.softfruver.inventario.service.UsuarioService;
import com.softfruver.inventario.user.Usuario;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/config")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class ConfigController {

  private final UsuarioService usuarioService;

  public ConfigController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  // Pantalla principal 
  @GetMapping
  public String pantalla(Model model) {
    List<Usuario> usuarios = usuarioService.listarTodos();
    model.addAttribute("usuarios", usuarios);
    return "config/index";
  }

  //  Crear secretaria 
  @PostMapping("/usuarios/crear-secretaria")
  public String crearSecretaria(@RequestParam String username,
                                @RequestParam String password,
                                RedirectAttributes ra) {
    try {
      usuarioService.crearSecretaria(username, password);
      ra.addFlashAttribute("ok", "Secretaria creada correctamente.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("err", e.getMessage());
    } catch (DataIntegrityViolationException e) {
      ra.addFlashAttribute("err", "El usuario ya existe.");
    }
    return "redirect:/config";
  }

  //  Editar 
  @GetMapping("/usuarios/{id}/editar")
  public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
    try {
      Usuario usuario = usuarioService.getById(id);
      // La vista espera 'usuario'
      model.addAttribute("usuario", usuario);
      return "config/editar";
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("err", e.getMessage());
      return "redirect:/config";
    }
  }

  // Actualizar desde la página de edición 
  @PostMapping("/usuarios/{id}/actualizar")
  public String actualizarUsuario(@PathVariable Long id,
                                  @RequestParam(name = "username", required = false) String username,
                                  @RequestParam(name = "nuevaPassword", required = false) String nuevaPassword,
                                  RedirectAttributes ra) {
    try {
      // El servicio acepta (id, nuevoUsername, nuevaPassword)
      usuarioService.actualizarUsuario(id, username, nuevaPassword);
      ra.addFlashAttribute("ok", "Usuario actualizado.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("err", e.getMessage());
      return "redirect:/config/usuarios/" + id + "/editar";
    } catch (DataIntegrityViolationException e) {
      ra.addFlashAttribute("err", "El nombre de usuario ya existe.");
      return "redirect:/config/usuarios/" + id + "/editar";
    }
    return "redirect:/config";
  }

  //  Activar / Desactivar 
  @PostMapping("/usuarios/{id}/desactivar")
  public String desactivar(@PathVariable Long id, RedirectAttributes ra) {
    try {
      usuarioService.setActivo(id, false);
      ra.addFlashAttribute("ok", "Usuario desactivado.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("err", e.getMessage());
    }
    return "redirect:/config";
  }

  @PostMapping("/usuarios/{id}/activar")
  public String activar(@PathVariable Long id, RedirectAttributes ra) {
    try {
      usuarioService.setActivo(id, true);
      ra.addFlashAttribute("ok", "Usuario activado.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("err", e.getMessage());
    }
    return "redirect:/config";
  }

  // Cambiar mi password 
  @PostMapping("/mi-password")
  public String cambiarMiPassword(@RequestParam(name = "nuevaPassword", required = false) String nuevaPassword,
                                  RedirectAttributes ra) {
    try {
      if (!StringUtils.hasText(nuevaPassword)) {
        ra.addFlashAttribute("err", "Debes escribir la nueva contraseña.");
        return "redirect:/config";
      }
      usuarioService.cambiarMiPassword(nuevaPassword);
      ra.addFlashAttribute("ok", "Tu contraseña fue actualizada.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("err", e.getMessage());
    }
    return "redirect:/config";
  }

  //  Borrar 
  @PostMapping("/usuarios/{id}/borrar")
  public String borrar(@PathVariable Long id, RedirectAttributes ra) {
    try {
      usuarioService.borrarFisicamenteSecretaria(id);
      ra.addFlashAttribute("ok", "Usuario eliminado definitivamente.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("err", e.getMessage());
    }
    return "redirect:/config";
  }
}
