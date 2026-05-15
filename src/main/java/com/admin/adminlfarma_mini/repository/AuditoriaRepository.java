package com.admin.adminlfarma_mini.repository;

import com.admin.adminlfarma_mini.entity.Auditoria;
import com.admin.adminlfarma_mini.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    // Owner ve todo
    List<Auditoria> findAllByOrderByFechaHoraDesc();

    // Admin ve acciones de empleados
    List<Auditoria> findByUsuarioRolOrderByFechaHoraDesc(String rol);

    // Empleado ve sus propias acciones
    List<Auditoria> findByUsuarioOrderByFechaHoraDesc(Usuario usuario);

    // Filtro por rango de fechas
    List<Auditoria> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime desde, LocalDateTime hasta);

    // Filtro por módulo
    List<Auditoria> findByModuloOrderByFechaHoraDesc(String modulo);
}
