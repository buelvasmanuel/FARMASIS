package com.admin.adminlfarma_mini.repository;

import com.admin.adminlfarma_mini.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends MongoRepository<Producto, String> {

    Optional<Producto> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    // Buscar productos activos (activo = true, no null)
    @Query("{ 'activo': true }")
    Page<Producto> findActivos(Pageable pageable);

    // Buscar productos archivados (activo = false)
    @Query("{ 'activo': false }")
    Page<Producto> findInactivos(Pageable pageable);

    // Método para búsqueda por nombre o código (Activos)
    @Query("{ $or: [ { 'nombre': { $regex: ?0, $options: 'i' } }, { 'codigo': { $regex: ?1, $options: 'i' } } ], 'activo': true }")
    Page<Producto> searchByNombreOrCodigo(String nombre, String codigo, Pageable pageable);

    // Método para búsqueda por nombre o código (Archivados)
    @Query("{ $or: [ { 'nombre': { $regex: ?0, $options: 'i' } }, { 'codigo': { $regex: ?1, $options: 'i' } } ], 'activo': false }")
    Page<Producto> searchByNombreOrCodigoInactivo(String nombre, String codigo, Pageable pageable);

    // Productos disponibles (activos con stock > 0) — para compatibilidad
    @Query("{ 'activo': true, 'cantidad': { $gt: 0 } }")
    List<Producto> findProductosDisponibles();

    // Todos los productos activos (incluidos stock 0) — para POS con bloqueo visual
    @Query("{ 'activo': true }")
    List<Producto> findProductosActivos();

    // POS paginado — todos los activos (incluye stock 0 para mostrar en gris)
    @Query("{ 'activo': true }")
    Page<Producto> findProductosActivosPaginado(Pageable pageable);

    // POS paginado — filtro por categoría
    @Query("{ 'activo': true, 'categoria': ?0 }")
    Page<Producto> findProductosActivosPorCategoria(String categoria, Pageable pageable);

    List<Producto> findByCantidadLessThan(Integer stockMinimo);

    // Motor de Optimización: productos activos con stock <= su stockMinimo individual (con fallback de 5 si es nulo)
    @Query("{ 'activo': true, $expr: { $lte: [ '$cantidad', { $ifNull: [ '$stockMinimo', 5 ] } ] } }")
    List<Producto> findProductosBajoStock();

    @Query("{ 'activo': true, $expr: { $lte: [ '$cantidad', { $ifNull: [ '$stockMinimo', 5 ] } ] } }")
    Page<Producto> findBajoStock(Pageable pageable);

    @Query("{ 'activo': true, 'categoria': ?0, $expr: { $lte: [ '$cantidad', { $ifNull: [ '$stockMinimo', 5 ] } ] } }")
    Page<Producto> findBajoStockPorCategoria(String categoria, Pageable pageable);

    @Query("{ $or: [ { 'nombre': { $regex: ?0, $options: 'i' } }, { 'codigo': { $regex: ?1, $options: 'i' } } ], 'activo': true, $expr: { $lte: [ '$cantidad', { $ifNull: [ '$stockMinimo', 5 ] } ] } }")
    Page<Producto> searchBajoStock(String nombre, String codigo, Pageable pageable);

    @Query("{ $or: [ { 'nombre': { $regex: ?0, $options: 'i' } }, { 'codigo': { $regex: ?1, $options: 'i' } } ], 'categoria': ?2, 'activo': true, $expr: { $lte: [ '$cantidad', { $ifNull: [ '$stockMinimo', 5 ] } ] } }")
    Page<Producto> searchBajoStockAndCategoria(String nombre, String codigo, String categoria, Pageable pageable);

    // Búsqueda en POS filtrada por categoría y término (nombre/código)
    @Query("{ $or: [ { 'nombre': { $regex: ?0, $options: 'i' } }, { 'codigo': { $regex: ?1, $options: 'i' } } ], 'categoria': ?2, 'activo': true }")
    Page<Producto> searchByNombreOrCodigoAndCategoria(String nombre, String codigo, String categoria, Pageable pageable);

    // CORREGIDO: Contar activos
    @Query(value = "{ 'activo': true }", count = true)
    long countActivos();

    // Método agregado para el Chatbot: buscar productos por aproximación de nombre
    List<Producto> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    // Módulo 4 — POS Categorías: filtrar por categoría
    @Query("{ 'categoria': ?0, 'activo': true, 'cantidad': { $gt: 0 } }")
    List<Producto> findByCategoriaAndDisponible(String categoria);

    // Módulo 4 — Obtener categorías distintas (se procesa en servicio)
    @Query(value = "{ 'activo': true, 'cantidad': { $gt: 0 } }", fields = "{ 'categoria': 1 }")
    List<Producto> findAllForCategorias();
}