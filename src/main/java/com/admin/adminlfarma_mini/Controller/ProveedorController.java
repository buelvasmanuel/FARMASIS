package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.Proveedor;
import com.admin.adminlfarma_mini.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.admin.adminlfarma_mini.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private EmailService emailService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public String listarProveedores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ACTIVOS") String estado,
            @RequestParam(required = false) String categoria,
            Model model, Authentication auth) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        Page<Proveedor> proveedoresPage = proveedorService.listarProveedores(search, estado, categoria, pageable);

        model.addAttribute("proveedores", proveedoresPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", proveedoresPage.getTotalPages());
        model.addAttribute("totalItems", proveedoresPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("estado", estado);
        model.addAttribute("categoria", categoria);
        model.addAttribute("categorias", proveedorService.obtenerCategorias());
        model.addAttribute("totalProveedores", proveedoresPage.getTotalElements());

        return "proveedores/listar";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public String mostrarFormNuevo(Model model) {
        model.addAttribute("proveedor", new Proveedor());
        model.addAttribute("titulo", "Nuevo Proveedor");
        model.addAttribute("formAction", "/proveedores/guardar");
        return "proveedores/form";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public String guardarProveedor(@Valid @ModelAttribute Proveedor proveedor,
            BindingResult result, Authentication auth,
            RedirectAttributes redirectAttributes, Model model) {
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

    @PostMapping("/actualizar/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public String actualizarProveedor(@PathVariable String id,
            @Valid @ModelAttribute Proveedor proveedor, BindingResult result,
            Model model, RedirectAttributes redirectAttributes) {
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

    // Desactivar proveedor (soft delete) — NO envía email
    @PostMapping("/desactivar/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public String desactivarProveedor(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            proveedorService.desactivarProveedor(id);
            redirectAttributes.addFlashAttribute("success", "Proveedor archivado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al archivar: " + e.getMessage());
        }
        return "redirect:/proveedores";
    }

    // Reactivar proveedor — NO envía email
    @PostMapping("/reactivar/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public String reactivarProveedor(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            proveedorService.reactivarProveedor(id);
            redirectAttributes.addFlashAttribute("success", "Proveedor reactivado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al reactivar: " + e.getMessage());
        }
        return "redirect:/proveedores?estado=ACTIVOS";
    }

    // Backward compat: old eliminar redirects to desactivar
    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public String eliminarProveedor(@PathVariable String id, RedirectAttributes redirectAttributes) {
        return desactivarProveedor(id, redirectAttributes);
    }
}