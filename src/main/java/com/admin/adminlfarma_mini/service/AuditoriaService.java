package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.Auditoria;
import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.repository.AuditoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    /** Registra un evento de auditoría de forma asíncrona para no bloquear la respuesta */
    @Async
    public void registrar(String accion, String detalle, String modulo,
                          Usuario usuario, HttpServletRequest request) {
        try {
            Auditoria log = new Auditoria();
            log.setAccion(accion);
            log.setDetalle(detalle);
            log.setModulo(modulo);
            log.setUsuario(usuario);
            if (request != null) {
                String ip = request.getHeader("X-Forwarded-For");
                log.setIp(ip != null ? ip.split(",")[0].trim() : request.getRemoteAddr());
            }
            auditoriaRepository.save(log);
        } catch (Exception e) {
            // No interrumpir el flujo principal por un error de auditoría
        }
    }

    /** Registra sin request (para uso interno sin contexto HTTP) */
    @Async
    public void registrar(String accion, String detalle, String modulo, Usuario usuario) {
        registrar(accion, detalle, modulo, usuario, null);
    }

    /** Devuelve logs filtrados según el rol del usuario que consulta */
    public List<Auditoria> getLogsParaUsuario(Usuario usuario) {
        if (usuario == null) return List.of();
        return switch (usuario.getRol()) {
            case "ROLE_OWNER" -> auditoriaRepository.findAllByOrderByFechaHoraDesc();
            case "ROLE_ADMIN" -> auditoriaRepository.findByUsuarioRolOrderByFechaHoraDesc("ROLE_EMPLEADO");
            default -> auditoriaRepository.findByUsuarioOrderByFechaHoraDesc(usuario);
        };
    }

    public List<Auditoria> getTodos() {
        return auditoriaRepository.findAllByOrderByFechaHoraDesc();
    }
}
