package com.admin.adminlfarma_mini.repository;

import com.admin.adminlfarma_mini.entity.Proveedor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProveedorRepository extends MongoRepository<Proveedor, String> {

    // Buscar proveedores activos
    List<Proveedor> findByActivoTrue();

    // Buscar por nombre (ignorando mayúsculas)
    List<Proveedor> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    // Buscar por empresa
    List<Proveedor> findByEmpresaContainingIgnoreCaseAndActivoTrue(String empresa);

    // Buscar por tipo de productos
    List<Proveedor> findByTipoProductosContainingIgnoreCaseAndActivoTrue(String tipoProductos);

    // Búsqueda personalizada con query
    @Query("{ 'activo': true, $or: [ " +
            "{ 'nombre': { $regex: ?0, $options: 'i' } }, " +
            "{ 'empresa': { $regex: ?0, $options: 'i' } }, " +
            "{ 'tipo_productos': { $regex: ?0, $options: 'i' } } ] }")
    List<Proveedor> buscarPorTexto(String texto);
}
