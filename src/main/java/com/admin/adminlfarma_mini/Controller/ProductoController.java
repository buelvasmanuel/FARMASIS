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

import jakarta.servlet.http.HttpServletRequest;

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
            @RequestParam(required = false) Boolean stockBajo,
            @RequestParam(required = false) Boolean enOferta,
            @RequestParam(defaultValue = "lista") String view,
            Model model,
            HttpServletRequest request) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "codigo"));
        var productosPage = productoService.buscarProductos(search, categoria, stockBajo, enOferta, pageable);

        model.addAttribute("productos", productosPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productosPage.getTotalPages());
        model.addAttribute("totalItems", productosPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("categoria", categoria);
        model.addAttribute("stockBajo", stockBajo);
        model.addAttribute("enOferta", enOferta);
        model.addAttribute("view", view);
        model.addAttribute("proveedores", proveedorService.listarTodos());
        model.addAttribute("stockBajoCount", productoService.getProductosBajoStock().size());

        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return "productos :: #productosContainer";
        }

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
            @RequestParam(required = false) Boolean stockBajo,
            @RequestParam(required = false) Boolean enOferta,
            @RequestParam(required = false) String view,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        String referer = request.getHeader("Referer");
        String redirectTarget = (referer != null ? referer : "/productos");

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
                return "redirect:" + redirectTarget;
            }
            if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El nombre es requerido");
                return "redirect:" + redirectTarget;
            }
            if (producto.getPrecio() == null || producto.getPrecio() <= 0) {
                redirectAttributes.addFlashAttribute("error", "El precio debe ser mayor a 0");
                return "redirect:" + redirectTarget;
            }

            // Validar código único solo para nuevos productos
            if (producto.getId() == null && productoService.existeCodigo(producto.getCodigo())) {
                redirectAttributes.addFlashAttribute("error", "Ya existe un producto registrado con este código");
                return "redirect:" + redirectTarget;
            }

            productoService.guardar(producto);
            redirectAttributes.addFlashAttribute("success", "Producto guardado correctamente");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un producto registrado con este código");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:" + redirectTarget;
    }

    @PostMapping({"/actualizar/{id}", "/actualizar"})
    public String actualizarProducto(@PathVariable(required = false) String id,
            @ModelAttribute Producto producto,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean stockBajo,
            @RequestParam(required = false) Boolean enOferta,
            @RequestParam(required = false) String view,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
            
        String referer = request.getHeader("Referer");
        String redirectTarget = (referer != null ? referer : "/productos");

        String targetId = id;
        if (targetId == null || targetId.trim().isEmpty()) {
            targetId = producto.getId();
        }

        if (targetId == null || targetId.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ID de producto no proporcionado");
            return "redirect:" + redirectTarget;
        }

        try {
            // Preservar la imagen anterior si no se sube una nueva
            final String finalId = targetId;
            productoService.obtenerPorId(finalId).ifPresent(pExistente -> {
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
            productoService.actualizar(targetId, producto);
            redirectAttributes.addFlashAttribute("success", "Producto actualizado correctamente");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un producto registrado con este código");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:" + redirectTarget;
    }

    @GetMapping("/archivados")
    @PreAuthorize("hasAnyAuthority('OWNER', 'ROLE_OWNER')")
    public String listarProductosArchivados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String search,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "codigo"));
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
        return String.format("redirect:/productos?page=%d&size=%d%s", page, size,
                (search != null && !search.isEmpty() ? "&search=" + search : ""));
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

    @PostMapping("/aplicar-descuento/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('OWNER', 'ROLE_OWNER')")
    public ResponseEntity<Map<String, Object>> aplicarDescuento(@PathVariable String id, @RequestParam Double porcentaje) {
        Map<String, Object> response = new HashMap<>();
        try {
            java.util.Optional<Producto> opt = productoService.obtenerPorId(id);
            if (opt.isPresent()) {
                Producto p = opt.get();
                if (p.getPrecioOriginal() == null) {
                    p.setPrecioOriginal(p.getPrecio());
                }
                double nuevoPrecio = p.getPrecioOriginal() * (1.0 - (porcentaje / 100.0));
                nuevoPrecio = Math.round(nuevoPrecio * 100.0) / 100.0;
                p.setPrecio(nuevoPrecio);
                productoService.actualizar(id, p);
                
                response.put("success", true);
                response.put("nuevoPrecio", nuevoPrecio);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Producto no encontrado");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al aplicar descuento: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/revertir-descuento")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('OWNER', 'ROLE_OWNER')")
    public ResponseEntity<Map<String, Object>> revertirDescuento(@RequestParam String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            java.util.Optional<Producto> opt = productoService.obtenerPorId(id);
            if (opt.isPresent()) {
                Producto p = opt.get();
                if (p.getPrecioOriginal() != null) {
                    p.setPrecio(p.getPrecioOriginal());
                    p.setPrecioOriginal(null);
                    productoService.actualizar(id, p);
                }
                response.put("success", true);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Producto no encontrado");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al revertir descuento: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}