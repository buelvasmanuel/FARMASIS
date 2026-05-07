package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class TwoFactorAuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    private static final SecureRandom random = new SecureRandom();

    public String generarCodigo() {
        return String.format("%04d", random.nextInt(10000));
    }

    public void enviarYGuardarCodigo(Usuario usuario) {
        String codigo = generarCodigo();
        usuario.setCodigo2FA(codigo);
        usuario.setCodigoExpiracion(LocalDateTime.now().plusMinutes(5));
        usuario.setCodigoVerificado(false);
        usuarioRepository.save(usuario);

        // Enviar email con el código
        if (usuario.getEmail() != null && !usuario.getEmail().isEmpty()) {
            try {
                emailService.enviarCodigoVerificacion(usuario.getEmail(), codigo);
            } catch (Exception e) {
                throw new RuntimeException("Error crítico de Gmail: No se pudo enviar el correo a " + usuario.getEmail() + ". Verifica que la contraseña de aplicación de Google sea correcta o que Gmail no esté bloqueando el acceso.", e);
            }
        }
    }

    public boolean verificarCodigo(Usuario usuario, String codigoIngresado) {
        if (usuario.getCodigo2FA() == null || usuario.getCodigoExpiracion() == null) {
            return false;
        }

        boolean valido = usuario.getCodigo2FA().equals(codigoIngresado) &&
                usuario.getCodigoExpiracion().isAfter(LocalDateTime.now());

        if (valido) {
            usuario.setCodigoVerificado(true);
            usuario.setCodigo2FA(null);
            usuario.setCodigoExpiracion(null);
            usuarioRepository.save(usuario);
        }

        return valido;
    }
}