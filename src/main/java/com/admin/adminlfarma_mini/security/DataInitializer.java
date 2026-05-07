package com.admin.adminlfarma_mini.security;

import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.repository.UsuarioRepository;
import com.admin.adminlfarma_mini.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            // Crear OWNER o restaurar contraseña si ya existe
            if (usuarioRepository.findFirstByUsername("manueljavier2016@gmail.com").isEmpty()) {
                log.info("👑 Creando OWNER (Superadministrador)...");
                usuarioService.crearOwner(
                        "manueljavier2016@gmail.com", // username ahora es el email
                        "admin123", // password simplificado
                        "manueljavier2016@gmail.com" // email para 2FA
                );
                log.info("✅ OWNER creado: usuario='manueljavier2016@gmail.com', contraseña='admin123'");
                log.info("⚠️ IMPORTANTE: Podrás recibir tu código de 4 dígitos a este correo.");
            } else {
                log.info("🔧 Restaurando acceso del OWNER por defecto...");
                Usuario owner = usuarioRepository.findFirstByUsername("manueljavier2016@gmail.com").get();
                owner.setPassword(passwordEncoder.encode("admin123"));
                owner.setActivo(true);
                owner.setRol("ROLE_OWNER");
                usuarioRepository.save(owner);
                log.info("✅ Clave del OWNER reestablecida a 'admin123'.");
            }

            // En DataInitializer.java, agregar:
            if (usuarioRepository.findFirstByUsername("empleado1").isEmpty()) {
                Usuario empleado = new Usuario();
                empleado.setUsername("empleado1");
                empleado.setPassword("empleado123");
                empleado.setRol("ROLE_EMPLOYEE");
                empleado.setEmail("empleado@example.com");
                usuarioService.registrar(empleado);
                log.info("✅ EMPLEADO creado: usuario='empleado1', contraseña='empleado123'");
            }
            // Crear ADMIN por defecto si no existe el usuario "admin"
            if (usuarioRepository.findFirstByUsername("admin").isEmpty()) {
                log.info("🔧 Creando ADMIN por defecto...");
                Usuario admin = new Usuario();
                admin.setUsername("admin");
                admin.setPassword("admin123");
                admin.setRol("ROLE_ADMIN");
                admin.setEmail("admin@example.com");
                usuarioService.registrar(admin);
                log.info("✅ ADMIN creado: usuario='admin', contraseña='admin123'");
            }
        };
    }
}