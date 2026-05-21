package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.Producto;
import com.admin.adminlfarma_mini.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.PageImpl;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final MongoTemplate mongoTemplate;

    public Page<Producto> listarProductos(Pageable pageable) {
        return productoRepository.findActivos(pageable);
    }

    public Page<Producto> listarProductosInactivos(Pageable pageable) {
        return productoRepository.findInactivos(pageable);
    }

    public Page<Producto> buscarProductos(String search, String categoria, Boolean stockBajo, Boolean enOferta, Pageable pageable) {
        boolean hasSearch = (search != null && !search.trim().isEmpty());
        boolean hasCategoria = (categoria != null && !categoria.trim().isEmpty());
        boolean isStockBajo = (stockBajo != null && stockBajo);
        boolean isEnOferta = (enOferta != null && enOferta);

        Query query = new Query().with(pageable);
        List<Criteria> criterios = new ArrayList<>();

        // Siempre filtrar por activos
        criterios.add(Criteria.where("activo").is(true));

        if (hasSearch) {
            String regexSearch = search.trim();
            criterios.add(new Criteria().orOperator(
                Criteria.where("nombre").regex(regexSearch, "i"),
                Criteria.where("codigo").regex(regexSearch, "i")
            ));
        }

        if (hasCategoria) {
            criterios.add(Criteria.where("categoria").is(categoria.trim()));
        }

        if (isStockBajo) {
            criterios.add(Criteria.where("$expr").is(new org.bson.Document("$lte", java.util.Arrays.asList("$cantidad", new org.bson.Document("$ifNull", java.util.Arrays.asList("$stockMinimo", 5))))));
        }

        if (isEnOferta) {
            criterios.add(Criteria.where("precioOriginal").ne(null));
        }

        query.addCriteria(new Criteria().andOperator(criterios.toArray(new Criteria[0])));

        List<Producto> productos = mongoTemplate.find(query, Producto.class);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Producto.class);

        return new PageImpl<>(productos, pageable, total);
    }

    public Page<Producto> buscarProductosInactivos(String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return listarProductosInactivos(pageable);
        }
        return productoRepository.searchByNombreOrCodigoInactivo(search, search, pageable);
    }

    public List<Producto> getProductosDisponibles() {
        return productoRepository.findProductosDisponibles();
    }

    // POS: Todos los activos (incluido stock 0 para mostrar en gris)
    public List<Producto> getProductosParaPOS() {
        return productoRepository.findProductosActivos();
    }

    // POS paginado
    public Page<Producto> getProductosParaPOS(Pageable pageable) {
        return productoRepository.findProductosActivosPaginado(pageable);
    }

    // POS paginado con filtro por categoría
    public Page<Producto> getProductosParaPOSPorCategoria(String categoria, Pageable pageable) {
        return productoRepository.findProductosActivosPorCategoria(categoria, pageable);
    }

    // POS paginado con búsqueda por término (nombre o código)
    public Page<Producto> getProductosParaPOSConBusqueda(String search, Pageable pageable) {
        return productoRepository.searchByNombreOrCodigo(search, search, pageable);
    }

    // POS paginado con búsqueda por término y categoría
    public Page<Producto> getProductosParaPOSConBusquedaYCategoria(String search, String categoria, Pageable pageable) {
        return productoRepository.searchByNombreOrCodigoAndCategoria(search, search, categoria, pageable);
    }

    // Obtener categorías únicas de productos activos
    public List<String> obtenerCategorias() {
        return productoRepository.findProductosActivos().stream()
                .map(Producto::getCategoria)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // Productos bajo stock (para Motor de Optimización)
    public List<Producto> getProductosBajoStock() {
        return productoRepository.findProductosBajoStock();
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
        if (producto.getStockMinimo() == null) {
            producto.setStockMinimo(5);
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
        if (productoActualizado.getStockMinimo() == null) {
            productoActualizado.setStockMinimo(5);
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

    public void restaurar(String id) {
        Optional<Producto> productoOpt = productoRepository.findById(id);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            producto.setActivo(true);
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