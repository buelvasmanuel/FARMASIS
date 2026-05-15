package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.Producto;
import com.admin.adminlfarma_mini.service.ProductoService;
import com.admin.adminlfarma_mini.service.ProveedorService;
import com.admin.adminlfarma_mini.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
    private final CloudinaryService cloudinaryService;

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
    public String guardarProducto(@ModelAttribute Producto producto, 
                                 @RequestParam(value = "file", required = false) MultipartFile file,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "15") int size,
                                 @RequestParam(required = false) String search,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Normalizar: el form envía "" (string vacío) cuando no hay id, no null
            if (producto.getId() != null && producto.getId().trim().isEmpty()) {
                producto.setId(null);
            }

            // Si es edición, obtener el producto actual para preservar campos no presentes en el form (como imagenUrl)
            if (producto.getId() != null) {
                productoService.obtenerPorId(producto.getId()).ifPresent(pExistente -> {
                    if (producto.getImagenUrl() == null) {
                        producto.setImagenUrl(pExistente.getImagenUrl());
                    }
                });
            }

            // Subir imagen si existe (esto sobreescribe la URL anterior si se sube algo nuevo)
            if (file != null && !file.isEmpty()) {
                String url = cloudinaryService.subirImagen(file);
                if (url != null) {
                    producto.setImagenUrl(url);
                }
            }

            // Validar campos requeridos
            if (producto.getCodigo() == null || producto.getCodigo().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El código es requerido");
                return String.format("redirect:/productos?page=%d&size=%d%s", page, size, (search != null && !search.isEmpty() ? "&search=" + search : ""));
            }
            if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El nombre es requerido");
                return String.format("redirect:/productos?page=%d&size=%d%s", page, size, (search != null && !search.isEmpty() ? "&search=" + search : ""));
            }
            if (producto.getPrecio() == null || producto.getPrecio() <= 0) {
                redirectAttributes.addFlashAttribute("error", "El precio debe ser mayor a 0");
                return String.format("redirect:/productos?page=%d&size=%d%s", page, size, (search != null && !search.isEmpty() ? "&search=" + search : ""));
            }

            // Validar código único solo para nuevos productos
            if (producto.getId() == null && productoService.existeCodigo(producto.getCodigo())) {
                redirectAttributes.addFlashAttribute("error", "Ya existe un producto registrado con este código");
                return String.format("redirect:/productos?page=%d&size=%d%s", page, size, (search != null && !search.isEmpty() ? "&search=" + search : ""));
            }

            productoService.guardar(producto);
            redirectAttributes.addFlashAttribute("success", "Producto guardado correctamente");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un producto registrado con este código");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            e.printStackTrace();
        }
        return String.format("redirect:/productos?page=%d&size=%d%s", page, size, (search != null && !search.isEmpty() ? "&search=" + search : ""));
    }

    @PostMapping("/actualizar/{id}")
    public String actualizarProducto(@PathVariable String id, 
                                   @ModelAttribute Producto producto,
                                   @RequestParam(value = "file", required = false) MultipartFile file,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "15") int size,
                                   @RequestParam(required = false) String search,
                                   RedirectAttributes redirectAttributes) {
        try {
            // Preservar la imagen anterior si no se sube una nueva
            productoService.obtenerPorId(id).ifPresent(pExistente -> {
                if (producto.getImagenUrl() == null) {
                    producto.setImagenUrl(pExistente.getImagenUrl());
                }
            });

            // Subir imagen si existe
            if (file != null && !file.isEmpty()) {
                String url = cloudinaryService.subirImagen(file);
                if (url != null) {
                    producto.setImagenUrl(url);
                }
            }
            productoService.actualizar(id, producto);
            redirectAttributes.addFlashAttribute("success", "Producto actualizado correctamente");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un producto registrado con este código");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            e.printStackTrace();
        }
        return String.format("redirect:/productos?page=%d&size=%d%s", page, size, (search != null && !search.isEmpty() ? "&search=" + search : ""));
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable String id, 
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "15") int size,
                                 @RequestParam(required = false) String search,
                                 RedirectAttributes redirectAttributes) {
        try {
            productoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Producto eliminado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return String.format("redirect:/productos?page=%d&size=%d%s", page, size, (search != null && !search.isEmpty() ? "&search=" + search : ""));
    }

    @GetMapping("/verificar-codigo")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> verificarCodigo(@RequestParam String codigo) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", productoService.existeCodigo(codigo));
        return ResponseEntity.ok(response);
    }
}