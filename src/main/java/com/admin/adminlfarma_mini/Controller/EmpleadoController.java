package com.admin.adminlfarma_mini.Controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/empleado")
@PreAuthorize("hasRole('EMPLEADO')")
public class EmpleadoController {

    // Redirigir al POS unificado
    @GetMapping("/pos")
    public String puntoDeVenta() {
        return "redirect:/ventas/nueva";
    }
}
