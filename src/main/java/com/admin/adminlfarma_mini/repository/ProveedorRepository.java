package com.admin.adminlfarma_mini.repository;

import com.admin.adminlfarma_mini.entity.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProveedorRepository extends MongoRepository<Proveedor, String> {

    // Buscar proveedores activos
    List<Proveedor> findByActivoTrue();

    // Paginado por estado
    Page<Proveedor> findByActivoTrue(Pageable pageable);
    Page<Proveedor> findByActivoFalse(Pageable pageable);

    // Buscar por nombre (ignorando mayúsculas)
    List<Proveedor> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    // Buscar por empresa
    List<Proveedor> findByEmpresaContainingIgnoreCaseAndActivoTrue(String empresa);

    // Buscar por tipo de productos
    List<Proveedor> findByTipoProductosContainingIgnoreCaseAndActivoTrue(String tipoProductos);

    // Búsqueda con filtro de estado — activos
    @Query("{ 'activo': true, $or: [ " +
            "{ 'nombre': { $regex: ?0, $options: 'i' } }, " +
            "{ 'empresa': { $regex: ?0, $options: 'i' } }, " +
            "{ 'tipo_productos': { $regex: ?0, $options: 'i' } } ] }")
    List<Proveedor> buscarPorTexto(String texto);

    // Búsqueda paginada — solo activos
    @Query("{ 'activo': true, $or: [ " +
            "{ 'nombre': { $regex: ?0, $options: 'i' } }, " +
            "{ 'empresa': { $regex: ?0, $options: 'i' } }, " +
            "{ 'tipo_productos': { $regex: ?0, $options: 'i' } } ] }")
    Page<Proveedor> buscarPorTextoPaginado(String texto, Pageable pageable);

    // Búsqueda paginada — solo archivados
    @Query("{ 'activo': false, $or: [ " +
            "{ 'nombre': { $regex: ?0, $options: 'i' } }, " +
            "{ 'empresa': { $regex: ?0, $options: 'i' } }, " +
            "{ 'tipo_productos': { $regex: ?0, $options: 'i' } } ] }")
    Page<Proveedor> buscarPorTextoArchivados(String texto, Pageable pageable);

    // Búsqueda paginada — activos + filtro categoría
    @Query("{ 'activo': true, 'tipo_productos': { $regex: ?1, $options: 'i' }, $or: [ " +
            "{ 'nombre': { $regex: ?0, $options: 'i' } }, " +
            "{ 'empresa': { $regex: ?0, $options: 'i' } } ] }")
    Page<Proveedor> buscarPorTextoYCategoria(String texto, String categoria, Pageable pageable);

    // Búsqueda paginada — archivados + filtro categoría
    @Query("{ 'activo': false, 'tipo_productos': { $regex: ?1, $options: 'i' }, $or: [ " +
            "{ 'nombre': { $regex: ?0, $options: 'i' } }, " +
            "{ 'empresa': { $regex: ?0, $options: 'i' } } ] }")
    Page<Proveedor> buscarPorTextoYCategoriaArchivados(String texto, String categoria, Pageable pageable);

    // Solo filtro categoría — activos
    @Query("{ 'activo': true, 'tipo_productos': { $regex: ?0, $options: 'i' } }")
    Page<Proveedor> findByActivoTrueAndTipoProductos(String categoria, Pageable pageable);

    // Solo filtro categoría — archivados
    @Query("{ 'activo': false, 'tipo_productos': { $regex: ?0, $options: 'i' } }")
    Page<Proveedor> findByActivoFalseAndTipoProductos(String categoria, Pageable pageable);

    // Categorías distintas
    @Aggregation(pipeline = {
        "{ $match: { 'tipo_productos': { $ne: null } } }",
        "{ $group: { _id: '$tipo_productos' } }",
        "{ $sort: { _id: 1 } }"
    })
    List<String> findDistinctTipoProductos();
}
