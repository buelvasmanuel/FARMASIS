package com.admin.adminlfarma_mini.repository;

import com.admin.adminlfarma_mini.entity.Novedad;
import com.admin.adminlfarma_mini.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NovedadRepository extends JpaRepository<Novedad, Long> {

    // Mis novedades (empleado ve las suyas)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario"})
    List<Novedad> findByUsuarioOrderByFechaRegistroDesc(Usuario usuario);

    // Pendientes para que admin/owner las gestione
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario"})
    List<Novedad> findByEstadoOrderByFechaRegistroAsc(Novedad.Estado estado);

    // Todas (para owner)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario"})
    List<Novedad> findAllByOrderByFechaRegistroDesc();

    // Novedades de un rol específico (admin ve solo empleados)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuario"})
    List<Novedad> findByUsuarioRolOrderByFechaRegistroDesc(String rol);
}
