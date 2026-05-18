package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.Proveedor;
import com.admin.adminlfarma_mini.repository.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    @Autowired
    private ProveedorRepository proveedorRepository;

    // Listar todos los proveedores activos (sin paginar — para selects)
    public List<Proveedor> listarTodos() {
        return proveedorRepository.findByActivoTrue();
    }

    // Listar paginado con filtros
    public Page<Proveedor> listarProveedores(String search, String estado, String categoria, Pageable pageable) {
        boolean activos = !"ARCHIVADOS".equals(estado);
        boolean tieneSearch = search != null && !search.trim().isEmpty();
        boolean tieneCategoria = categoria != null && !categoria.trim().isEmpty();

        if (tieneSearch && tieneCategoria) {
            return activos
                    ? proveedorRepository.buscarPorTextoYCategoria(search, categoria, pageable)
                    : proveedorRepository.buscarPorTextoYCategoriaArchivados(search, categoria, pageable);
        } else if (tieneSearch) {
            return activos
                    ? proveedorRepository.buscarPorTextoPaginado(search, pageable)
                    : proveedorRepository.buscarPorTextoArchivados(search, pageable);
        } else if (tieneCategoria) {
            return activos
                    ? proveedorRepository.findByActivoTrueAndTipoProductos(categoria, pageable)
                    : proveedorRepository.findByActivoFalseAndTipoProductos(categoria, pageable);
        } else {
            return activos
                    ? proveedorRepository.findByActivoTrue(pageable)
                    : proveedorRepository.findByActivoFalse(pageable);
        }
    }

    // Buscar por ID
    public Optional<Proveedor> buscarPorId(String id) {
        return proveedorRepository.findById(id);
    }

    // Crear nuevo proveedor
    public Proveedor crearProveedor(Proveedor proveedor, String creadoPor) {
        proveedor.setFechaCreacion(LocalDateTime.now());
        proveedor.setFechaActualizacion(LocalDateTime.now());
        proveedor.setActivo(true);
        proveedor.setCreadoPor(creadoPor);
        return proveedorRepository.save(proveedor);
    }

    // Actualizar proveedor
    public Proveedor actualizarProveedor(String id, Proveedor proveedorActualizado) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        proveedor.setNombre(proveedorActualizado.getNombre());
        proveedor.setEmpresa(proveedorActualizado.getEmpresa());
        proveedor.setTelefono(proveedorActualizado.getTelefono());
        proveedor.setEmail(proveedorActualizado.getEmail());
        proveedor.setDireccion(proveedorActualizado.getDireccion());
        proveedor.setTipoProductos(proveedorActualizado.getTipoProductos());
        proveedor.setFechaActualizacion(LocalDateTime.now());

        return proveedorRepository.save(proveedor);
    }

    // Desactivar proveedor (soft delete) — SIN email
    public void desactivarProveedor(String id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        proveedor.setActivo(false);
        proveedor.setFechaActualizacion(LocalDateTime.now());
        proveedorRepository.save(proveedor);
    }

    // Reactivar proveedor — SIN email
    public void reactivarProveedor(String id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        proveedor.setActivo(true);
        proveedor.setFechaActualizacion(LocalDateTime.now());
        proveedorRepository.save(proveedor);
    }

    // Eliminar proveedor (soft delete) — mantener compatibilidad
    public void eliminarProveedor(String id) {
        desactivarProveedor(id);
    }

    // Eliminar proveedor permanentemente
    public void eliminarPermanentemente(String id) {
        proveedorRepository.deleteById(id);
    }

    // Buscar por texto
    public List<Proveedor> buscarPorTexto(String texto) {
        if (texto == null || texto.isEmpty()) {
            return listarTodos();
        }
        return proveedorRepository.buscarPorTexto(texto);
    }

    // Contar proveedores activos
    public long contarProveedores() {
        return proveedorRepository.findByActivoTrue().size();
    }

    // Obtener categorías únicas dinámicas desde la base de datos
    public List<String> obtenerCategorias() {
        return proveedorRepository.findAll().stream()
                .map(Proveedor::getTipoProductos)
                .filter(cat -> cat != null && !cat.trim().isEmpty())
                .distinct()
                .sorted()
                .toList();
    }
}