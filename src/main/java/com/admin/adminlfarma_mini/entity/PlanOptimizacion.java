package com.admin.adminlfarma_mini.entity;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Clase @PlanningSolution para el modelo de optimización de inventario.
 * Contiene la lista de propuestas de pedido y los rangos de valores
 * que Timefold usará para optimizar las variables de planificación.
 */
@PlanningSolution
@Data
@NoArgsConstructor
public class PlanOptimizacion {

    /**
     * Lista de propuestas de pedido que el solver optimizará.
     */
    @PlanningEntityCollectionProperty
    private List<PropuestaPedido> propuestas;

    /**
     * Score de la solución calculado por Timefold.
     * Hard: Restricciones que NO se pueden violar (presupuesto, almacén, etc.)
     * Soft: Función objetivo a maximizar (utilidad total)
     */
    @PlanningScore
    private HardSoftScore score;

    // ==================== VALUE RANGE PROVIDERS ====================

    /**
     * Rango de valores para cantidadAPedir: 0 a 500 (inclusive).
     * Timefold evaluará cada valor entero en este rango.
     */
    @ValueRangeProvider(id = "cantidadRange")
    public CountableValueRange<Integer> getCantidadRange() {
        return ValueRangeFactory.createIntValueRange(0, 501); // [0, 500]
    }

    /**
     * Rango de valores para descuentoProximidad: 0.0 a 0.5 con saltos de 0.05.
     * Se provee como lista explícita para control preciso de los valores discretos.
     * Valores: 0.0, 0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50
     */
    @ValueRangeProvider(id = "descuentoRange")
    public List<Double> getDescuentoRange() {
        return List.of(
                0.00, 0.05, 0.10, 0.15, 0.20,
                0.25, 0.30, 0.35, 0.40, 0.45, 0.50
        );
    }

    // ==================== CONSTRUCTOR DE CONVENIENCIA ====================

    public PlanOptimizacion(List<PropuestaPedido> propuestas) {
        this.propuestas = propuestas;
    }
}
