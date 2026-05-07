package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/login")
    public String loginForm(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
        }
        if (logout != null) {
            model.addAttribute("message", "Has cerrado sesión correctamente");
        }
        return "login";
    }

    // Opcional: Ruta para crear el primer administrador si no existe ninguno
    @GetMapping("/register-admin")
    public String showRegisterForm(Model model, RedirectAttributes redirectAttributes) {
        if (usuarioService.existeAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un administrador en el sistema.");
            return "redirect:/login";
        }
        return "register-admin";
    }

    @PostMapping("/register-admin")
    public String registerAdmin(@RequestParam String username,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {
        try {
            if (usuarioService.existeUsuario(username)) {
                redirectAttributes.addFlashAttribute("error", "El nombre de usuario ya está en uso.");
                return "redirect:/register-admin";
            }

            Usuario nuevoAdmin = new Usuario();
            nuevoAdmin.setUsername(username);
            nuevoAdmin.setPassword(password);
            nuevoAdmin.setRol("ADMIN");

            usuarioService.registrar(nuevoAdmin);
            redirectAttributes.addFlashAttribute("success", "Administrador registrado exitosamente. Inicia sesión.");
            return "redirect:/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error en el registro: " + e.getMessage());
            return "redirect:/register-admin";
        }
    }
}