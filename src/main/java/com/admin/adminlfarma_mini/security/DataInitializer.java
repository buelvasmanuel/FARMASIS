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
            // Crear OWNER si no existe ninguno en la base de datos (Fail-safe)
            if (!usuarioRepository.existsByRol("ROLE_OWNER")) {
                log.info("👑 No se detectó ningún OWNER. Creando Superadministrador inicial...");
                usuarioService.crearOwner(
                        "juan.garces028@gmail.com", // username inicial
                        "Admin123!", // password inicial
                        "juan.garces028@gmail.com" // email inicial
                );
                log.info("✅ OWNER inicial creado: usuario='juan.garces028@gmail.com', contraseña='Admin123!'");
                log.info("⚠️ IMPORTANTE: Por favor cambie sus credenciales en el módulo de Configuración.");
            } else {
                log.info("👑 El sistema ya cuenta con un OWNER. Saltando inicialización.");
            }
        };
    }
}