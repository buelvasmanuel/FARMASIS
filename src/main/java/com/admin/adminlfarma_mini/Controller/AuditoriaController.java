package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.service.AuditoriaService;
import com.admin.adminlfarma_mini.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final UsuarioService usuarioService;

    private Usuario getUsuarioActual(Authentication auth) {
        return usuarioService.buscarPorUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @GetMapping("/owner/auditoria")
    @PreAuthorize("hasRole('OWNER')")
    public String auditoriaOwner(Model model, Authentication auth) {
        model.addAttribute("logs", auditoriaService.getTodos());
        model.addAttribute("titulo", "Log de Auditoría Completo");
        return "auditoria/logs";
    }

    @GetMapping("/admin/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    public String auditoriaAdmin(Model model, Authentication auth) {
        Usuario usuario = getUsuarioActual(auth);
        model.addAttribute("logs", auditoriaService.getLogsParaUsuario(usuario));
        model.addAttribute("titulo", "Actividad de Empleados");
        return "auditoria/logs";
    }

    @GetMapping("/empleado/mis-actividades")
    @PreAuthorize("hasRole('EMPLEADO')")
    public String misActividades(Model model, Authentication auth) {
        Usuario usuario = getUsuarioActual(auth);
        model.addAttribute("logs", auditoriaService.getLogsParaUsuario(usuario));
        model.addAttribute("titulo", "Mis Actividades");
        return "auditoria/logs";
    }
}
