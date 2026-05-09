package com.admin.adminlfarma_mini.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "configuracion_sistema")
@Data
public class ConfiguracionSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_farmacia")
    private String nombreFarmacia = "FARMASIS";

    @Column(name = "nit")
    private String nit = "123456789-0";

    @Column(name = "direccion")
    private String direccion = "Calle Principal #123";

    @Column(name = "telefono")
    private String telefono = "300 000 0000";

    @Column(name = "umbral_stock_bajo")
    private Integer umbralStockBajo = 5;

    public ConfiguracionSistema() {
    }
}
