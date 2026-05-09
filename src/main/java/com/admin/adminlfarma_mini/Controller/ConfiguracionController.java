package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ConfiguracionController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final com.admin.adminlfarma_mini.service.ConfiguracionService configuracionService;

    @GetMapping("/configuracion")
    public String configuracion(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = usuarioService.buscarPorUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        model.addAttribute("usuario", usuario);
        model.addAttribute("config", configuracionService.getConfiguracion());
        return "configuracion";
    }

    @PostMapping("/configuracion/actualizar")
    public String actualizarPerfil(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String email,
            @RequestParam(required = false) String passwordActual,
            @RequestParam(required = false) String nuevaPassword,
            @RequestParam(required = false) String fotoPerfil,
            RedirectAttributes redirectAttributes) {

        try {
            Usuario usuario = usuarioService.buscarPorUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Si intenta cambiar la contraseña o el email
            if ((nuevaPassword != null && !nuevaPassword.isEmpty()) || !email.equals(usuario.getEmail())) {
                if (passwordActual == null || passwordActual.isEmpty()) {
                    throw new RuntimeException("Debe ingresar la contraseña actual para realizar cambios sensibles (Email/Password)");
                }
                if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                    throw new RuntimeException("La contraseña actual es incorrecta");
                }
            }

            usuarioService.actualizarPerfil(usuario.getId(), email, nuevaPassword, fotoPerfil);
            redirectAttributes.addFlashAttribute("success", "Perfil actualizado exitosamente.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/configuracion";
    }

    @PostMapping("/configuracion/sistema")
    public String actualizarSistema(
            com.admin.adminlfarma_mini.entity.ConfiguracionSistema config,
            RedirectAttributes redirectAttributes) {
        try {
            configuracionService.guardarConfiguracion(config);
            redirectAttributes.addFlashAttribute("success", "Configuración del sistema actualizada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar la configuración: " + e.getMessage());
        }
        return "redirect:/configuracion";
    }
}
