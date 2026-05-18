package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findFirstByUsername(username);
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public List<Usuario> listarTodosActivos() {
        return usuarioRepository.findByActivoTrue();
    }

    public List<Usuario> listarTodosIncluyendoInactivos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarPorRol(String rol) {
        return usuarioRepository.findByRolAndActivoTrue(rol);
    }

    public List<Usuario> buscarPorRol(String rolFiltro) {
        if (rolFiltro == null || rolFiltro.isEmpty() || "TODOS".equals(rolFiltro)) {
            return listarTodosActivos();
        }
        return usuarioRepository.findUsuariosByRolContaining(rolFiltro);
    }

    public boolean existeUsuario(String username) {
        return usuarioRepository.findFirstByUsername(username).isPresent();
    }

    public boolean existeAdmin() {
        return !usuarioRepository.findByRolAndActivoTrue("ROLE_ADMIN").isEmpty() ||
                !usuarioRepository.findByRolAndActivoTrue("ADMIN").isEmpty();
    }

    public boolean existeOwner() {
        return !usuarioRepository.findByRolAndActivoTrue("ROLE_OWNER").isEmpty();
    }

    @Transactional
    public Usuario registrar(Usuario usuario) {
        validarPassword(usuario.getPassword());
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        if (usuario.getRol() == null || usuario.getRol().isEmpty()) {
            usuario.setRol("ROLE_EMPLEADO");
        } else if (!usuario.getRol().startsWith("ROLE_")) {
            usuario.setRol("ROLE_" + usuario.getRol());
        }
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizar(Usuario usuario) {
        // Buscar el rol anterior antes de guardar para detectar si cambió
        String rolAnterior = null;
        if (usuario.getId() != null) {
            rolAnterior = usuarioRepository.findRolById(usuario.getId());
        }

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // Si el rol cambió, enviar correo asíncrono
        if (rolAnterior != null && !rolAnterior.equals(usuarioGuardado.getRol())) {
            enviarCorreoPorCambioRol(usuarioGuardado, rolAnterior, usuarioGuardado.getRol());
        }

        return usuarioGuardado;
    }

    @Transactional
    public Usuario actualizarRol(Long id, String nuevoRol) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar que no se pueda modificar al OWNER
        if ("ROLE_OWNER".equals(usuario.getRol())) {
            throw new RuntimeException("No se puede modificar el rol del OWNER");
        }

        if (usuario.getFechaUltimoCambioRol() != null) {
            long minutosTranscurridos = java.time.Duration
                    .between(usuario.getFechaUltimoCambioRol(), LocalDateTime.now()).toMinutes();
            if (minutosTranscurridos < 5) {
                long minutosRestantes = 5 - minutosTranscurridos;
                throw new RuntimeException("Por seguridad, debes esperar " + minutosRestantes
                        + " minuto(s) antes de volver a cambiar el rol de este usuario");
            }
        }

        String rolAnterior = usuario.getRol();
        String rolConPrefijo = nuevoRol.startsWith("ROLE_") ? nuevoRol : "ROLE_" + nuevoRol;
        usuario.setRol(rolConPrefijo);
        usuario.setFechaUltimoCambioRol(LocalDateTime.now());
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // Enviar correo de notificación del cambio de rol DESPUÉS de guardar en la BD
        if (rolAnterior != null && !rolAnterior.equals(usuarioGuardado.getRol())) {
            enviarCorreoPorCambioRol(usuarioGuardado, rolAnterior, usuarioGuardado.getRol());
        }

        return usuarioGuardado;
    }

    private void enviarCorreoPorCambioRol(Usuario usuarioGuardado, String rolAnterior, String rolNuevo) {
        if (usuarioGuardado.getEmail() != null && !usuarioGuardado.getEmail().isEmpty()) {
            String nombreCompleto = (usuarioGuardado.getNombre() != null ? usuarioGuardado.getNombre() : "") + " " + (usuarioGuardado.getApellido() != null ? usuarioGuardado.getApellido() : "");
            nombreCompleto = nombreCompleto.trim();
            if (nombreCompleto.isEmpty()) {
                nombreCompleto = usuarioGuardado.getUsername();
            }
            try {
                if (rolAnterior != null && rolAnterior.contains("EMPLEADO") && rolNuevo.contains("ADMIN")) {
                    emailService.enviarCorreoAscenso(usuarioGuardado.getEmail(), nombreCompleto);
                } else if (rolAnterior != null && rolAnterior.contains("ADMIN") && rolNuevo.contains("EMPLEADO")) {
                    emailService.enviarCorreoAjusteRol(usuarioGuardado.getEmail(), nombreCompleto);
                } else {
                    emailService.enviarCorreoActualizacionRolGenerico(usuarioGuardado.getEmail(), nombreCompleto, rolNuevo);
                }
            } catch (Exception e) {
                System.err.println("Error al enviar correo de actualización de rol: " + e.getMessage());
            }
        }
    }

    @Transactional
    public Usuario desactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // No permitir desactivar al OWNER
        if ("ROLE_OWNER".equals(usuario.getRol())) {
            throw new RuntimeException("No se puede desactivar al OWNER del sistema");
        }

        if (usuario.getFechaUltimoCambioEstado() != null) {
            long minutosTranscurridos = java.time.Duration
                    .between(usuario.getFechaUltimoCambioEstado(), LocalDateTime.now()).toMinutes();
            if (minutosTranscurridos < 5) {
                long minutosRestantes = 5 - minutosTranscurridos;
                throw new RuntimeException("Por seguridad, debes esperar " + minutosRestantes
                        + " minuto(s) antes de volver a cambiar el estado de este usuario");
            }
        }

        usuario.setActivo(false);
        usuario.setFechaUltimoCambioEstado(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario activarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.getFechaUltimoCambioEstado() != null) {
            long minutosTranscurridos = java.time.Duration
                    .between(usuario.getFechaUltimoCambioEstado(), LocalDateTime.now()).toMinutes();
            if (minutosTranscurridos < 5) {
                long minutosRestantes = 5 - minutosTranscurridos;
                throw new RuntimeException("Por seguridad, debes esperar " + minutosRestantes
                        + " minuto(s) antes de volver a cambiar el estado de este usuario");
            }
        }

        usuario.setActivo(true);
        usuario.setFechaUltimoCambioEstado(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario crearOwner(String username, String password, String email) {
        validarPassword(password);
        Usuario owner = new Usuario();
        owner.setUsername(username);
        owner.setPassword(passwordEncoder.encode(password));
        owner.setRol("ROLE_OWNER");
        owner.setEmail(email);
        owner.setEmailVerificado(true);
        owner.setCodigoVerificado(false);
        owner.setActivo(true);
        owner.setFechaCreacion(LocalDateTime.now());
        return usuarioRepository.save(owner);
    }

    public long contarPorRol(String rol) {
        return usuarioRepository.findByRolAndActivoTrue(rol).size();
    }

    public long contarActivos() {
        return usuarioRepository.countByRolNotAndActivoTrue("ROLE_OWNER");
    }

    public long contarInactivos() {
        return usuarioRepository.findAll().stream().filter(u -> !Boolean.TRUE.equals(u.getActivo())).count();
    }

    @Transactional
    public Usuario actualizarPerfil(Long id, String email, String password, String fotoPerfil) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (email != null && !email.isEmpty()) {
            usuario.setEmail(email);
            usuario.setUsername(email); // Mantener consistencia si el username es el email
        }

        if (password != null && !password.isEmpty()) {
            validarPassword(password);
            usuario.setPassword(passwordEncoder.encode(password));
        }

        if (fotoPerfil != null) {
            usuario.setFotoPerfil(fotoPerfil);
        }

        return usuarioRepository.save(usuario);
    }

    public void validarPassword(String password) {
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
        if (password == null || !password.matches(regex)) {
            throw new RuntimeException(
                    "La contraseña debe tener al menos 8 caracteres, incluir un número y un carácter especial.");
        }
    }
}
