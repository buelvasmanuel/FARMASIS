package com.admin.adminlfarma_mini.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Document(collection = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    private String id;

    @Indexed(unique = true)
    private String codigo;

    private String nombre;
    private String email;
    private String telefono;

    @Indexed(unique = true)
    private String identificacion;

    private String direccion;
    private String username;
    private String direccionFisica;
    private Boolean esConsumidorFinal = false;
}