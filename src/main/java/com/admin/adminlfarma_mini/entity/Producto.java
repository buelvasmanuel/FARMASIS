package com.admin.adminlfarma_mini.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Document(collection = "PRODUCTOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    private String id;

    @Indexed(unique = true)
    @jakarta.validation.constraints.Pattern(regexp = "^[0-9]{4}$", message = "El código debe tener exactamente 4 dígitos (ej. 0001)")
    private String codigo;

    private String nombre;
    private Double precio;
    private Double costoCompra;
    private Integer cantidad;
    private String categoria;
    private String presentacion;
    private String concentracion;
    private String lote;
    private String principiosActivos;
    private LocalDate fechaVencimiento;
    private String proveedorId;
    private String descripcion;
    private String imagenUrl;
    private LocalDate fechaIngreso;
    private Boolean activo = true;
}