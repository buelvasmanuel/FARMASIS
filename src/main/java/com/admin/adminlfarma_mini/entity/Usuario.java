// Actualizar Usuario.java - Añadir campo activo y más roles
package com.admin.adminlfarma_mini.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String rol; // ROLE_OWNER, ROLE_ADMIN, ROLE_EMPLEADO

    @Column(name = "email")
    private String email;

    @Column(name = "nombre", length = 100)
    private String nombre;

    @Column(name = "apellido", length = 100)
    private String apellido;

    @Column(name = "email_verificado")
    private Boolean emailVerificado = false;

    @Column(name = "activo")
    private Boolean activo = true; // Para soft delete

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_ultimo_cambio_rol")
    private LocalDateTime fechaUltimoCambioRol;

    @Column(name = "fecha_ultimo_cambio_estado")
    private LocalDateTime fechaUltimoCambioEstado;

    // Campos 2FA
    @Column(name = "codigo_2fa")
    private String codigo2FA;

    @Column(name = "codigo_expiracion")
    private LocalDateTime codigoExpiracion;

    @Column(name = "codigo_verificado")
    private Boolean codigoVerificado = false;

    @Lob
    @Column(name = "foto_perfil", columnDefinition = "LONGTEXT")
    private String fotoPerfil;

    // Constructores
    public Usuario() {
        this.fechaCreacion = LocalDateTime.now();
        this.activo = true;
    }

    public Usuario(String username, String password, String rol) {
        this();
        this.username = username;
        this.password = password;
        this.rol = rol;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public Boolean getEmailVerificado() {
        return emailVerificado;
    }

    public void setEmailVerificado(Boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaUltimoCambioRol() {
        return fechaUltimoCambioRol;
    }

    public void setFechaUltimoCambioRol(LocalDateTime fechaUltimoCambioRol) {
        this.fechaUltimoCambioRol = fechaUltimoCambioRol;
    }

    public LocalDateTime getFechaUltimoCambioEstado() {
        return fechaUltimoCambioEstado;
    }

    public void setFechaUltimoCambioEstado(LocalDateTime fechaUltimoCambioEstado) {
        this.fechaUltimoCambioEstado = fechaUltimoCambioEstado;
    }

    public String getCodigo2FA() {
        return codigo2FA;
    }

    public void setCodigo2FA(String codigo2FA) {
        this.codigo2FA = codigo2FA;
    }

    public LocalDateTime getCodigoExpiracion() {
        return codigoExpiracion;
    }

    public void setCodigoExpiracion(LocalDateTime codigoExpiracion) {
        this.codigoExpiracion = codigoExpiracion;
    }

    public Boolean getCodigoVerificado() {
        return codigoVerificado;
    }

    public void setCodigoVerificado(Boolean codigoVerificado) {
        this.codigoVerificado = codigoVerificado;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }
}