package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.Novedad;
import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.repository.NovedadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NovedadService {

    private final NovedadRepository novedadRepository;

    /** Empleado reporta una novedad */
    @Transactional
    public Novedad reportar(Novedad novedad, Usuario solicitante) {
        novedad.setUsuario(solicitante);
        novedad.setEstado(Novedad.Estado.PENDIENTE);
        novedad.setFechaRegistro(LocalDateTime.now());
        return novedadRepository.save(novedad);
    }

    /** Admin/Owner aprueba una novedad */
    @Transactional
    public Novedad aprobar(Long id, Usuario revisor) {
        Novedad novedad = novedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Novedad no encontrada"));
        novedad.setEstado(Novedad.Estado.APROBADO);
        novedad.setRevisadoPor(revisor);
        return novedadRepository.save(novedad);
    }

    /** Admin/Owner rechaza una novedad */
    @Transactional
    public Novedad rechazar(Long id, String comentario, Usuario revisor) {
        Novedad novedad = novedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Novedad no encontrada"));
        novedad.setEstado(Novedad.Estado.RECHAZADO);
        novedad.setRevisadoPor(revisor);
        novedad.setComentarioAdmin(comentario);
        return novedadRepository.save(novedad);
    }

    /** Mis novedades (empleado) */
    public List<Novedad> getMisNovedades(Usuario usuario) {
        return novedadRepository.findByUsuarioOrderByFechaRegistroDesc(usuario);
    }

    /** Novedades pendientes (admin/owner) */
    public List<Novedad> getPendientes() {
        return novedadRepository.findByEstadoOrderByFechaRegistroAsc(Novedad.Estado.PENDIENTE);
    }

    /** Todas las novedades (owner) */
    public List<Novedad> getTodas() {
        return novedadRepository.findAllByOrderByFechaRegistroDesc();
    }

    /** Novedades de empleados (admin) */
    public List<Novedad> getDeEmpleados() {
        return novedadRepository.findByUsuarioRolOrderByFechaRegistroDesc("ROLE_EMPLEADO");
    }

    public Novedad findById(Long id) {
        return novedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Novedad no encontrada"));
    }
}
