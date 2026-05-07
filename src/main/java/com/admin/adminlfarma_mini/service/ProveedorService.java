package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.Proveedor;
import com.admin.adminlfarma_mini.repository.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    @Autowired
    private ProveedorRepository proveedorRepository;

    // Listar todos los proveedores activos
    public List<Proveedor> listarTodos() {
        return proveedorRepository.findByActivoTrue();
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

    // Eliminar proveedor (soft delete)
    public void eliminarProveedor(String id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
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
}