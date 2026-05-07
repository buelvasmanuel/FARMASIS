package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.Producto;
import com.admin.adminlfarma_mini.service.ProductoService;
import com.admin.adminlfarma_mini.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;
    private final ProveedorService proveedorService;

    @GetMapping
    public String listarProductos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String search,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        var productosPage = productoService.buscarProductos(search, pageable);

        model.addAttribute("productos", productosPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productosPage.getTotalPages());
        model.addAttribute("totalItems", productosPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("proveedores", proveedorService.listarTodos());

        return "productos";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Producto> obtenerProducto(@PathVariable String id) {
        return productoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/guardar")
    public String guardarProducto(@ModelAttribute Producto producto, RedirectAttributes redirectAttributes) {
        try {
            // Normalizar: el form envía "" (string vacío) cuando no hay id, no null
            if (producto.getId() != null && producto.getId().trim().isEmpty()) {
                producto.setId(null);
            }

            // Validar campos requeridos
            if (producto.getCodigo() == null || producto.getCodigo().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El código es requerido");
                return "redirect:/productos";
            }
            if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El nombre es requerido");
                return "redirect:/productos";
            }
            if (producto.getPrecio() == null || producto.getPrecio() <= 0) {
                redirectAttributes.addFlashAttribute("error", "El precio debe ser mayor a 0");
                return "redirect:/productos";
            }

            // Validar código único solo para nuevos productos
            if (producto.getId() == null && productoService.existeCodigo(producto.getCodigo())) {
                redirectAttributes.addFlashAttribute("error", "Ya existe un producto registrado con este código");
                return "redirect:/productos";
            }

            productoService.guardar(producto);
            redirectAttributes.addFlashAttribute("success", "Producto guardado correctamente");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un producto registrado con este código");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/productos";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizarProducto(@PathVariable String id, @ModelAttribute Producto producto,
            RedirectAttributes redirectAttributes) {
        try {
            productoService.actualizar(id, producto);
            redirectAttributes.addFlashAttribute("success", "Producto actualizado correctamente");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un producto registrado con este código");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/productos";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            productoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Producto eliminado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/productos";
    }

    @GetMapping("/verificar-codigo")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> verificarCodigo(@RequestParam String codigo) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", productoService.existeCodigo(codigo));
        return ResponseEntity.ok(response);
    }
}