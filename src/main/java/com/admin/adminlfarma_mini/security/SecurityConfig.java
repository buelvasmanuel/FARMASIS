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
                                                                "/webjars/**", "/favicon.ico")
                                                .permitAll()
                                                // Rutas protegidas estrictamente
                                                .requestMatchers("/owner/**").hasRole("OWNER")
                                                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OWNER")
                                                .requestMatchers("/empleado/**").hasRole("EMPLEADO")
                                                .requestMatchers("/configuracion/**").hasAnyRole("OWNER", "ADMIN")
                                                .requestMatchers("/api/optimizar").hasRole("OWNER")
                                                .requestMatchers("/api/optimizacion/**").hasRole("OWNER")
                                                // POS unificado: accesible por todos los roles
                                                .requestMatchers("/ventas/nueva", "/ventas/guardar").authenticated()
                                                .requestMatchers("/api/chat/**").authenticated()
                                                .requestMatchers("/api/**").authenticated()
                                                .requestMatchers("/novedades/**").authenticated()
                                                .requestMatchers("/asistencia/**").authenticated()
                                                .requestMatchers("/auditoria/**").authenticated()
                                                .anyRequest().authenticated())
                                .headers(headers -> headers
                                                // Habilitamos protección contra caché para evitar que el botón 'Atrás' muestre datos sensibles
                                                .cacheControl(org.springframework.security.config.Customizer.withDefaults())
                                                .frameOptions(frame -> frame.sameOrigin()))
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/verificar-codigo", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll());

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}