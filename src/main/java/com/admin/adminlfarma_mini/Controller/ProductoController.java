package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.Producto;
import com.admin.adminlfarma_mini.service.CloudinaryService;
import com.admin.adminlfarma_mini.service.ProductoService;
import com.admin.adminlfarma_mini.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "lista") String view,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        var productosPage = productoService.buscarProductos(search, categoria, pageable);

        model.addAttribute("productos", productosPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productosPage.getTotalPages());
        model.addAttribute("totalItems", productosPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("categoria", categoria);
        model.addAttribute("view", view);
        model.addAttribute("proveedores", proveedorService.listarTodos());
        model.addAttribute("stockBajoCount", productoService.getProductosBajoStock().size());

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
            @RequestParam(required = false) String categoria,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);
        if (search != null && !search.isEmpty()) {
            redirectAttributes.addAttribute("search", search);
        }
        if (categoria != null && !categoria.isEmpty()) {
            redirectAttributes.addAttribute("categoria", categoria);
        }

        try {
            // Normalizar: el form envía "" (string vacío) cuando no hay id, no null
            if (producto.getId() != null && producto.getId().trim().isEmpty()) {
                producto.setId(null);
            }

            // Si es edición, obtener el producto actual para preservar campos no presentes
            // en el form (como imagenUrl)
            if (producto.getId() != null) {
                productoService.obtenerPorId(producto.getId()).ifPresent(pExistente -> {
                    if (producto.getImagenUrl() == null) {
                        producto.setImagenUrl(pExistente.getImagenUrl());
                    }
                });
            }

            // Subir imagen a Cloudinary si se proporciona un archivo
            if (file != null && !file.isEmpty()) {
                String url = cloudinaryService.subirImagen(file);
                if (url != null) {
                    producto.setImagenUrl(url);
                }
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
    public String actualizarProducto(@PathVariable String id,
            @ModelAttribute Producto producto,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoria,
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
        return String.format("redirect:/productos?page=%d&size=%d%s%s", page, size,
                (search != null && !search.isEmpty() ? "&search=" + search : ""),
                (categoria != null && !categoria.isEmpty() ? "&categoria=" + categoria : ""));
    }

    @GetMapping("/archivados")
    @PreAuthorize("hasAnyAuthority('OWNER', 'ROLE_OWNER')")
    public String listarProductosArchivados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String search,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        var productosPage = productoService.buscarProductosInactivos(search, pageable);

        model.addAttribute("productos", productosPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productosPage.getTotalPages());
        model.addAttribute("totalItems", productosPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("archivadosMode", true); // Flag para identificar la vista
        model.addAttribute("proveedores", proveedorService.listarTodos());

        return "productos-archivados";
    }

    @PostMapping("/archivar/{id}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'ROLE_OWNER')")
    public String archivarProducto(@PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoria,
            RedirectAttributes redirectAttributes) {
        try {
            productoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Producto archivado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al archivar: " + e.getMessage());
        }
        return String.format("redirect:/productos?page=%d&size=%d%s%s", page, size,
                (search != null && !search.isEmpty() ? "&search=" + search : ""),
                (categoria != null && !categoria.isEmpty() ? "&categoria=" + categoria : ""));
    }

    @PostMapping("/restaurar/{id}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'ROLE_OWNER')")
    public String restaurarProducto(@PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            productoService.restaurar(id);
            redirectAttributes.addFlashAttribute("success", "Producto restaurado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al restaurar: " + e.getMessage());
        }
        return String.format("redirect:/productos/archivados?page=%d&size=%d%s", page, size,
                (search != null && !search.isEmpty() ? "&search=" + search : ""));
    }

    @GetMapping("/verificar-codigo")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> verificarCodigo(@RequestParam String codigo) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", productoService.existeCodigo(codigo));
        return ResponseEntity.ok(response);
    }
}