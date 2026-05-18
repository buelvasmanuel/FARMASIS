package com.admin.adminlfarma_mini.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Entidad para persistir el borrador de la última optimización.
 * Se mantiene un único registro que se sobrescribe en cada ejecución.
 */
@Document(collection = "resultado_optimizacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoOptimizacion {

    @Id
    private String id;

    private String score;
    private Integer hardScore;
    private Integer softScore;
    private List<Map<String, Object>> propuestas;
    private Map<String, Object> resumen;
    private LocalDateTime fechaCalculo;
}
