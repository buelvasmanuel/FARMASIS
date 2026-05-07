package com.admin.adminlfarma_mini.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @GetMapping("/dashboard")
    public String adminDashboard(Model model, Authentication auth) {
        String username = auth.getName();
        log.info("Admin {} accediendo al panel exclusivo", username);

        model.addAttribute("username", username);
        model.addAttribute("titulo", "Panel de Administración - L-Farma");
        return "admin/dashboard";
    }

    // Aquí puedes añadir más endpoints de gestión según necesites
    // @GetMapping("/usuarios")
    // public String gestionarUsuarios(Model model) { ... }
}