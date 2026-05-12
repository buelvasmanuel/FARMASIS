package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.Producto;
import com.admin.adminlfarma_mini.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    public Page<Producto> listarProductos(Pageable pageable) {
        // CORREGIDO: Solo productos activos
        return productoRepository.findActivos(pageable);
    }

    public Page<Producto> buscarProductos(String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return listarProductos(pageable);
        }
        // CORREGIDO: Búsqueda correcta
        return productoRepository.searchByNombreOrCodigo(search, search, pageable);
    }

    public List<Producto> getProductosDisponibles() {
        return productoRepository.findProductosDisponibles();
    }

    public long contarProductos() {
        return productoRepository.count();
    }

    public Optional<Producto> obtenerPorId(String id) {
        return productoRepository.findById(id);
    }

    public Optional<Producto> obtenerPorCodigo(String codigo) {
        return productoRepository.findByCodigo(codigo);
    }

    public boolean existeCodigo(String codigo) {
        return productoRepository.existsByCodigo(codigo);
    }

    public Producto guardar(Producto producto) {
        if (producto.getCostoCompra() != null && producto.getPrecio() <= producto.getCostoCompra()) {
            throw new IllegalArgumentException("El precio de venta debe ser mayor al costo de compra");
        }
        if (producto.getCodigo() != null && !producto.getCodigo().trim().isEmpty()) {
            try {
                producto.setCodigo(String.format("%04d", Integer.parseInt(producto.getCodigo())));
            } catch (NumberFormatException e) {
                // Ignore if it's not a valid integer format
            }
        }
        if (producto.getActivo() == null) {
            producto.setActivo(true);
        }
        return productoRepository.save(producto);
    }

    public Producto actualizar(String id, Producto productoActualizado) {
        productoActualizado.setId(id);
        if (productoActualizado.getCostoCompra() != null && productoActualizado.getPrecio() <= productoActualizado.getCostoCompra()) {
            throw new IllegalArgumentException("El precio de venta debe ser mayor al costo de compra");
        }
        if (productoActualizado.getCodigo() != null && !productoActualizado.getCodigo().trim().isEmpty()) {
            try {
                productoActualizado.setCodigo(String.format("%04d", Integer.parseInt(productoActualizado.getCodigo())));
            } catch (NumberFormatException e) {
                // Ignore if it's not a valid integer format
            }
        }
        if (productoActualizado.getActivo() == null) {
            productoActualizado.setActivo(true);
        }
        return productoRepository.save(productoActualizado);
    }

    public void eliminar(String id) {
        Optional<Producto> productoOpt = productoRepository.findById(id);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            producto.setActivo(false);
            productoRepository.save(producto);
        }
    }

    public void actualizarStock(String productoId, Integer cantidadVendida) {
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            producto.setCantidad(producto.getCantidad() - cantidadVendida);
            productoRepository.save(producto);
        }
    }

    public boolean validarStock(String productoId, Integer cantidad) {
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        return productoOpt.map(producto -> producto.getCantidad() >= cantidad).orElse(false);
    }
}