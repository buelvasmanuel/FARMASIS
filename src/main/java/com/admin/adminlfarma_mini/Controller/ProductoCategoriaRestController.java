package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.Producto;
import com.admin.adminlfarma_mini.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoCategoriaRestController {

    private final ProductoRepository productoRepository;

    /** Devuelve lista de categorías únicas con productos disponibles */
    @GetMapping("/categorias")
    public List<String> getCategorias() {
        return productoRepository.findAllForCategorias().stream()
                .map(Producto::getCategoria)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** Devuelve productos disponibles filtrados por categoría */
    @GetMapping("/por-categoria")
    public List<Map<String, Object>> getByCategoria(@RequestParam String categoria) {
        List<Producto> productos = categoria.equalsIgnoreCase("todos")
                ? productoRepository.findProductosDisponibles()
                : productoRepository.findByCategoriaAndDisponible(categoria);

        return productos.stream().map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "nombre", p.getNombre() != null ? p.getNombre() : "",
                "precio", p.getPrecio() != null ? p.getPrecio() : 0.0,
                "cantidad", p.getCantidad() != null ? p.getCantidad() : 0,
                "codigo", p.getCodigo() != null ? p.getCodigo() : "",
                "categoria", p.getCategoria() != null ? p.getCategoria() : ""
        )).collect(Collectors.toList());
    }
}
