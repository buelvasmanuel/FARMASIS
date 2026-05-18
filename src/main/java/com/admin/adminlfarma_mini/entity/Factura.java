package com.admin.adminlfarma_mini.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "FACTURA")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Factura {

    @Id
    private String id;

    @Indexed(unique = true)
    private String numeroFactura;

    private String clienteId; // ID del cliente en MongoDB
    private String clienteNombre; // Denormalizado para rápida consulta

    private LocalDateTime fecha;
    private Double subtotal;
    private Double iva = 0.0;
    private Double total;
    private String metodoPago = "EFECTIVO";
    private String observaciones;
    private String vendedorEmail;

    private List<DetalleFactura> detalles = new ArrayList<>();
}