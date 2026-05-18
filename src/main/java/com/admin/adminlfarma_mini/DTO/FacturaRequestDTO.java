package com.admin.adminlfarma_mini.DTO;

import lombok.Data;
import java.util.List;

@Data
public class FacturaRequestDTO {
    private String clienteId;
    private String clienteCC;
    private Double subtotal;
    private Double total;
    private String metodoPago;
    private String observaciones;
    private List<DetalleDTO> detalles;

    @Data
    public static class DetalleDTO {
        private String productoId;
        private Integer cantidad;
        private Double precioUnitario;
    }
}