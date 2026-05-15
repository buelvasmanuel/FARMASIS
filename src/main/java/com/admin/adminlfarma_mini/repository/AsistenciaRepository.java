package com.admin.adminlfarma_mini.repository;

import com.admin.adminlfarma_mini.entity.Asistencia;
import com.admin.adminlfarma_mini.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    // Registros del día (panel admin)
    List<Asistencia> findByFechaOrderByHoraEntradaAsc(LocalDate fecha);

    // Registros de un usuario en un rango de fechas (vista semanal)
    List<Asistencia> findByUsuarioAndFechaBetweenOrderByFechaAsc(
            Usuario usuario, LocalDate desde, LocalDate hasta);

    // Entrada abierta (sin salida) del día actual del usuario
    Optional<Asistencia> findByUsuarioAndFechaAndHoraSalidaIsNull(Usuario usuario, LocalDate fecha);

    // Historial de un usuario
    List<Asistencia> findByUsuarioOrderByFechaDescHoraEntradaDesc(Usuario usuario);

    // Para exportar Excel: todos los registros en rango + roles
    List<Asistencia> findByFechaBetweenOrderByFechaAscHoraEntradaAsc(LocalDate desde, LocalDate hasta);

    // Para exportar Excel filtrado por rol
    List<Asistencia> findByUsuarioRolInAndFechaBetweenOrderByFechaAsc(
            List<String> roles, LocalDate desde, LocalDate hasta);
}
