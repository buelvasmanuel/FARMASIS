package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.service.TwoFactorAuthService;
import com.admin.adminlfarma_mini.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TwoFactorController {

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/verificar-codigo")
    public String mostrarVerificacion(Authentication auth, Model model) {
        if (auth == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioService.buscarPorUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (usuario.getEmail() == null || usuario.getEmail().isEmpty()) {
            return "redirect:/configurar-email";
        }

        // Generar y enviar nuevo código
        twoFactorAuthService.enviarYGuardarCodigo(usuario);
        model.addAttribute("email", usuario.getEmail());
        return "verificar-codigo";
    }

    @PostMapping("/verificar-codigo")
    public String verificarCodigo(@RequestParam String codigo,
            Authentication auth,
            Model model) {
        Usuario usuario = usuarioService.buscarPorUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (twoFactorAuthService.verificarCodigo(usuario, codigo)) {
            // Redirigir según el rol
            if ("ROLE_OWNER".equals(usuario.getRol())) {
                return "redirect:/owner/dashboard";
            } else if ("ROLE_ADMIN".equals(usuario.getRol())) {
                return "redirect:/admin/dashboard";
            } else if ("ROLE_EMPLEADO".equals(usuario.getRol())) {
                return "redirect:/empleado/pos";
            }
            return "redirect:/login";
        }

        model.addAttribute("error", "Código inválido o expirado");
        model.addAttribute("email", usuario.getEmail());
        return "verificar-codigo";
    }

    @GetMapping("/configurar-email")
    public String configurarEmail(Authentication auth, Model model) {
        Usuario usuario = usuarioService.buscarPorUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        model.addAttribute("email", usuario.getEmail());
        return "configurar-email";
    }

    @PostMapping("/configurar-email")
    public String guardarEmail(@RequestParam String email,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioService.buscarPorUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setEmail(email);
        usuarioService.actualizar(usuario);
        redirectAttributes.addFlashAttribute("message", "Email configurado correctamente");
        return "redirect:/verificar-codigo";
    }
}