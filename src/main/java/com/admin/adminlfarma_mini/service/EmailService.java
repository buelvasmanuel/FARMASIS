package com.admin.adminlfarma_mini.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    public void enviarCodigoVerificacion(String destinatario, String codigo) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(destinatario);
        helper.setSubject("🔐 Código de Autenticación - AdminFarma");
        helper.setFrom("manueljavier2016@gmail.com", "AdminFarma Seguridad");
        
        String htmlMsg = "<div style='font-family: Arial, sans-serif; padding: 20px; max-width: 500px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 10px;'>"
                + "<h2 style='color: #4a5568; text-align: center;'>Verificación de Seguridad</h2>"
                + "<p style='color: #4a5568;'>Hola,</p>"
                + "<p style='color: #4a5568;'>Tu código de verificación seguro es:</p>"
                + "<div style='background-color: #f7fafc; border: 2px dashed #cbd5e0; padding: 15px; text-align: center; margin: 20px 0; border-radius: 5px;'>"
                + "<h1 style='color: #2b6cb0; margin: 0; letter-spacing: 5px; font-size: 36px;'>" + codigo + "</h1>"
                + "</div>"
                + "<p style='color: #718096; font-size: 14px;'>Este código expirará en 5 minutos.</p>"
                + "<p style='color: #718096; font-size: 12px; margin-top: 30px; text-align: center;'>Si no solicitaste este código, puedes ignorar este mensaje de forma segura.</p>"
                + "</div>";
                
        helper.setText(htmlMsg, true); // true indica que es HTML

        mailSender.send(message);
    }

    @org.springframework.scheduling.annotation.Async
    public void enviarCorreoBienvenida(String toEmail, String nombre, String username, String password, String rol) {
        try {
            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("nombre", nombre);
            context.setVariable("username", username);
            context.setVariable("password", password);
            context.setVariable("rol", rol.replace("ROLE_", ""));

            String process = templateEngine.process("email/welcome-email", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setText(process, true); // true indica que es HTML
            helper.setTo(toEmail);
            helper.setSubject("¡Bienvenido al equipo de L-farma!");

            mailSender.send(mimeMessage);
            System.out.println("Correo de bienvenida enviado exitosamente a: " + toEmail);
        } catch (jakarta.mail.MessagingException e) {
            System.err.println("Error al enviar el correo de bienvenida: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void enviarCorreoAscenso(String toEmail, String nombre) {
        try {
            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("nombre", nombre);

            String process = templateEngine.process("email/role-promotion-email", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setText(process, true);
            helper.setTo(toEmail);
            helper.setSubject("¡Felicidades! Has sido promovido a Administrador");

            mailSender.send(mimeMessage);
            System.out.println("Correo de ascenso enviado exitosamente a: " + toEmail);
        } catch (jakarta.mail.MessagingException e) {
            System.err.println("Error al enviar el correo de ascenso: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void enviarCorreoAjusteRol(String toEmail, String nombre) {
        try {
            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("nombre", nombre);

            String process = templateEngine.process("email/role-demotion-email", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setText(process, true);
            helper.setTo(toEmail);
            helper.setSubject("Notificación sobre tu rol en Farmasis");

            mailSender.send(mimeMessage);
            System.out.println("Correo de ajuste de rol enviado exitosamente a: " + toEmail);
        } catch (jakarta.mail.MessagingException e) {
            System.err.println("Error al enviar el correo de ajuste de rol: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void enviarCorreoReactivacion(String toEmail, String nombre) {
        try {
            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("nombre", nombre);

            String process = templateEngine.process("email/account-reactivated-email", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setText(process, true);
            helper.setTo(toEmail);
            helper.setSubject("Tu cuenta en Farmasis ha sido reactivada");

            mailSender.send(mimeMessage);
            System.out.println("Correo de reactivación enviado exitosamente a: " + toEmail);
        } catch (jakarta.mail.MessagingException e) {
            System.err.println("Error al enviar el correo de reactivación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void enviarCorreoSuspension(String toEmail, String nombre) {
        try {
            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("nombre", nombre);

            String process = templateEngine.process("email/account-suspended-email", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setText(process, true);
            helper.setTo(toEmail);
            helper.setSubject("Notificación importante sobre tu cuenta en Farmasis");

            mailSender.send(mimeMessage);
            System.out.println("Correo de suspensión enviado exitosamente a: " + toEmail);
        } catch (jakarta.mail.MessagingException e) {
            System.err.println("Error al enviar el correo de suspensión: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void enviarCorreoBienvenidaProveedor(String toEmail, String nombre, String empresa) {
        try {
            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("nombre", nombre);
            context.setVariable("empresa", empresa);

            String process = templateEngine.process("email/provider-welcome-email", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setText(process, true);
            helper.setTo(toEmail);
            helper.setSubject("Confirmación de registro como Proveedor - Farmasis");

            mailSender.send(mimeMessage);
            System.out.println("Correo de bienvenida a proveedor enviado exitosamente a: " + toEmail);
        } catch (jakarta.mail.MessagingException e) {
            System.err.println("Error al enviar el correo de bienvenida al proveedor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}