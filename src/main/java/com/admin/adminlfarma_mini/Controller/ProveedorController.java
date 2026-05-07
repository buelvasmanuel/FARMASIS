package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.Proveedor;
import com.admin.adminlfarma_mini.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import com.admin.adminlfarma_mini.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private EmailService emailService;

    // Listar proveedores (accesible para OWNER y ADMIN)
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public String listarProveedores(
            @RequestParam(value = "search", required = false) String search,
            Model model,
            Authentication auth) {

        List<Proveedor> proveedores;
        if (search != null && !search.isEmpty()) {
            proveedores = proveedorService.buscarPorTexto(search);
            model.addAttribute("searchTerm", search);
        } else {
            proveedores = proveedorService.listarTodos();
        }

        model.addAttribute("proveedores", proveedores);
        model.addAttribute("totalProveedores", proveedores.size());
        return "proveedores/listar";
    }

    // Mostrar formulario de creación
    @GetMapping("/nuevo")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public String mostrarFormNuevo(Model model) {
        model.addAttribute("proveedor", new Proveedor());
        model.addAttribute("titulo", "Nuevo Proveedor");
        model.addAttribute("formAction", "/proveedores/guardar");
        return "proveedores/form";
    }

    // Guardar nuevo proveedor
    @PostMapping("/guardar")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public String guardarProveedor(@Valid @ModelAttribute Proveedor proveedor,
            BindingResult result,
            Authentication auth,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("titulo", "Nuevo Proveedor");
            model.addAttribute("formAction", "/proveedores/guardar");
            return "proveedores/form";
        }
        try {
            String username = auth.getName();
            proveedorService.crearProveedor(proveedor, username);
            emailService.enviarCorreoBienvenidaProveedor(proveedor.getEmail(), proveedor.getNombre(), proveedor.getEmpresa());
            redirectAttributes.addFlashAttribute("success", "Proveedor creado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al crear proveedor: " + e.getMessage());
        }
        return "redirect:/proveedores";
    }

    // Mostrar formulario de edición
    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public String mostrarFormEditar(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Proveedor proveedor = proveedorService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
            model.addAttribute("proveedor", proveedor);
            model.addAttribute("titulo", "Editar Proveedor");
            model.addAttribute("formAction", "/proveedores/actualizar/" + id);
            return "proveedores/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Proveedor no encontrado");
            return "redirect:/proveedores";
        }
    }

    // Actualizar proveedor
    @PostMapping("/actualizar/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public String actualizarProveedor(@PathVariable String id,
            @Valid @ModelAttribute Proveedor proveedor,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("titulo", "Editar Proveedor");
            model.addAttribute("formAction", "/proveedores/actualizar/" + id);
            return "proveedores/form";
        }
        try {
            proveedorService.actualizarProveedor(id, proveedor);
            redirectAttributes.addFlashAttribute("success", "Proveedor actualizado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        return "redirect:/proveedores";
    }

    // Eliminar proveedor (soft delete)
    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public String eliminarProveedor(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            proveedorService.eliminarProveedor(id);
            redirectAttributes.addFlashAttribute("success", "Proveedor eliminado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/proveedores";
    }
}