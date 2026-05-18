package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.service.FacturaService;
import com.admin.adminlfarma_mini.service.ProductoService;
import com.admin.adminlfarma_mini.service.ProveedorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private FacturaService facturaService;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model, Authentication auth) {
        String username = auth.getName();
        log.info("Admin {} accediendo al panel", username);

        model.addAttribute("username", username);
        model.addAttribute("titulo", "Panel de Administración - L-Farma");
        model.addAttribute("totalProductos", productoService.contarProductos());
        model.addAttribute("totalProveedores", proveedorService.contarProveedores());
        model.addAttribute("ventasDelDia", facturaService.getVentasDelDia());
        model.addAttribute("cantidadVentasDelDia", facturaService.getCantidadVentasDelDia());
        return "admin/dashboard";
    }

    // Redirigir a vistas principales (con filtros y paginación)
    @GetMapping("/inventario")
    public String inventario() {
        return "redirect:/productos";
    }

    @GetMapping("/proveedores")
    public String proveedores() {
        return "redirect:/proveedores";
    }

    @GetMapping("/ventas")
    public String ventas() {
        return "redirect:/ventas";
    }
}