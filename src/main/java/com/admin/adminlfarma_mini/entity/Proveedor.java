package com.admin.adminlfarma_mini.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

@Document(collection = "proveedores")
public class Proveedor {

    @Id
    private String id;

    @Field("nombre")
    @NotBlank(message = "El nombre es requerido")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre no puede contener números ni símbolos")
    private String nombre;

    @Field("empresa")
    @NotBlank(message = "La empresa es requerida")
    private String empresa;

    @Field("telefono")
    @NotBlank(message = "El teléfono es requerido")
    @Pattern(regexp = "^3[0-9]{9}$", message = "Formato de celular colombiano inválido")
    private String telefono;

    @Field("email")
    @NotBlank(message = "El email es requerido")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    @Field("direccion")
    private String direccion;

    @Field("tipo_productos")
    private String tipoProductos;

    @Field("activo")
    private Boolean activo = true;

    @Field("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Field("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Field("creado_por")
    private String creadoPor;

    // Constructores
    public Proveedor() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        this.activo = true;
    }

    public Proveedor(String nombre, String empresa, String telefono, String email, String direccion,
            String tipoProductos) {
        this();
        this.nombre = nombre;
        this.empresa = empresa;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.tipoProductos = tipoProductos;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTipoProductos() {
        return tipoProductos;
    }

    public void setTipoProductos(String tipoProductos) {
        this.tipoProductos = tipoProductos;
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

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }
}