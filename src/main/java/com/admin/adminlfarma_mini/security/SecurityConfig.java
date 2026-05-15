package com.admin.adminlfarma_mini.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                .csrf(csrf -> csrf
                                .ignoringRequestMatchers("/api/chat/**", "/api/**",
                                        "/novedades/**", "/asistencia/**",
                                        "/admin/novedades/**", "/admin/asistencia/**",
                                        "/owner/asistencia/**"))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/login", "/css/**", "/js/**", "/images/**",
                                                                "/webjars/**", "/configurar-email", "/api/chat/**", "/api/**")
                                                .permitAll()
                                                // OWNER accede a todo (incluyendo /admin/**)
                                                .requestMatchers("/owner/**").hasRole("OWNER")
                                                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OWNER")
                                                .requestMatchers("/empleado/**").hasRole("EMPLEADO")
                                                .requestMatchers("/configuracion/**").hasAnyRole("OWNER", "ADMIN")
                                                // Módulos nuevos — cualquier usuario autenticado
                                                .requestMatchers("/novedades/**").authenticated()
                                                .requestMatchers("/asistencia/**").authenticated()
                                                .requestMatchers("/auditoria/**").authenticated()
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/verificar-codigo", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .permitAll());

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}