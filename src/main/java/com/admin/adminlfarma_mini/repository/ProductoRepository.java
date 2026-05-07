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

    // CORREGIDO: Buscar productos activos (activo = true, no null)
    @Query("{ 'activo': true }")
    Page<Producto> findActivos(Pageable pageable);

    // CORREGIDO: Método para búsqueda por nombre o código
    @Query("{ $or: [ { 'nombre': { $regex: ?0, $options: 'i' } }, { 'codigo': { $regex: ?1, $options: 'i' } } ], 'activo': true }")
    Page<Producto> searchByNombreOrCodigo(String nombre, String codigo, Pageable pageable);

    // CORREGIDO: Productos disponibles (activo true y cantidad > 0)
    @Query("{ 'activo': true, 'cantidad': { $gt: 0 } }")
    List<Producto> findProductosDisponibles();

    List<Producto> findByCantidadLessThan(Integer stockMinimo);

    // CORREGIDO: Contar activos
    @Query(value = "{ 'activo': true }", count = true)
    long countActivos();
}