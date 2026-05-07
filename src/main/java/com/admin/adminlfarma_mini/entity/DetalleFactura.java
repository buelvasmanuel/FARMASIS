package com.admin.adminlfarma_mini.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleFactura {
    private String productoId;
    private String productoNombre;
    private String productoCodigo;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
}