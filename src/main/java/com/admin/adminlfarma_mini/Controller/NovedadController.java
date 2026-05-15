package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.Novedad;
import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.service.AuditoriaService;
import com.admin.adminlfarma_mini.service.NovedadService;
import com.admin.adminlfarma_mini.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NovedadController {

    private final NovedadService novedadService;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;
    private final jakarta.servlet.http.HttpServletRequest request;

    private Usuario getUsuarioActual(Authentication auth) {
        if (auth == null) throw new RuntimeException("No autenticado");
        return usuarioService.buscarPorUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // ===================== EMPLEADO / TODOS =====================

    @GetMapping("/novedades/mis-novedades")
    public String misNovedades(Model model, Authentication auth) {
        Usuario usuario = getUsuarioActual(auth);
        model.addAttribute("novedades", novedadService.getMisNovedades(usuario));
        return "novedades/mis-novedades";
    }

    @GetMapping("/novedades/nueva")
    public String nuevaNovedadForm(Model model, Authentication auth) {
        log.info("Accediendo a formulario de nueva novedad - Usuario: {}", auth != null ? auth.getName() : "Anónimo");
        if (!model.containsAttribute("novedad")) {
            model.addAttribute("novedad", new Novedad());
        }
        model.addAttribute("tipos", Novedad.Tipo.values());
        return "novedades/nueva";
    }

    @PostMapping("/novedades/nueva")
    public String guardarNovedad(@Valid @ModelAttribute Novedad novedad,
                                  BindingResult result,
                                  Authentication auth,
                                  RedirectAttributes redirect,
                                  Model model) {
        log.info("Intento de reporte de novedad por {}", auth != null ? auth.getName() : "ANÓNIMO");
        if (result.hasErrors()) {
            log.warn("Errores de validación en novedad: {}", result.getAllErrors());
            model.addAttribute("tipos", Novedad.Tipo.values());
            return "novedades/nueva";
        }
        try {
            Usuario usuario = getUsuarioActual(auth);
            novedadService.reportar(novedad, usuario);
            auditoriaService.registrar("REPORTE_NOVEDAD", "Tipo: " + novedad.getTipo(), "RRHH", usuario, request);
            redirect.addFlashAttribute("successMsg", "Novedad reportada correctamente.");
            return "redirect:/novedades/mis-novedades";
        } catch (Exception e) {
            log.error("Error al reportar novedad", e);
            model.addAttribute("errorMsg", "Error técnico: " + e.getMessage());
            model.addAttribute("tipos", Novedad.Tipo.values());
            return "novedades/nueva";
        }
    }

    // ===================== ADMIN / OWNER =====================

    @GetMapping("/admin/novedades/pendientes")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public String pendientes(Model model, Authentication auth) {
        try {
            log.info("Accediendo a gestión de novedades - Usuario: {}", auth.getName());
            Usuario usuario = getUsuarioActual(auth);
            
            List<Novedad> novedades;
            if ("ROLE_OWNER".equals(usuario.getRol())) {
                novedades = novedadService.getTodas();
            } else {
                novedades = novedadService.getDeEmpleados();
            }
            
            model.addAttribute("novedades", novedades);
            model.addAttribute("pendienteCount", novedadService.getPendientes().size());
            return "novedades/admin-pendientes";
        } catch (Exception e) {
            log.error("Error en vista de pendientes", e);
            model.addAttribute("errorMsg", "Error al cargar novedades: " + e.getMessage());
            return "novedades/admin-pendientes";
        }
    }

    @PostMapping("/admin/novedades/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public String aprobar(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        novedadService.aprobar(id, getUsuarioActual(auth));
        redirect.addFlashAttribute("successMsg", "Novedad aprobada correctamente.");
        return "redirect:/admin/novedades/pendientes";
    }

    @PostMapping("/admin/novedades/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public String rechazar(@PathVariable Long id,
                           @RequestParam String comentario,
                           Authentication auth,
                           RedirectAttributes redirect) {
        novedadService.rechazar(id, comentario, getUsuarioActual(auth));
        redirect.addFlashAttribute("errorMsg", "Novedad rechazada.");
        return "redirect:/admin/novedades/pendientes";
    }
}
