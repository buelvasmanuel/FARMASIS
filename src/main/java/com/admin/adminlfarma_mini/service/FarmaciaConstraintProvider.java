package com.admin.adminlfarma_mini.service;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import com.admin.adminlfarma_mini.entity.PropuestaPedido;

/**
 * Proveedor de restricciones para el modelo de optimización de inventario.
 * 
 * Modelo Matemático de Programación No Lineal:
 * - Función Objetivo (Soft): Maximizar Utilidad = Ingresos - Costos - CostoAlmacenamiento
 * - R1 (Hard): Restricción de Presupuesto (≤ $1,500,000)
 * - R2 (Hard): Restricción de Almacén (≤ 5,000 unidades de espacio)
 * - R3 (Hard): Restricción de Demanda Mínima
 * - R4 (Hard): Restricción de Horas de Personal (≤ 160 horas)
 * - R5 (Hard): Restricción de Descuento Incoherente (no descontar si stock = 0)
 */
public class FarmaciaConstraintProvider implements ConstraintProvider {

    // ==================== CONSTANTES DEL MODELO ====================
    private static final int PRESUPUESTO_MAXIMO = 1_500_000;     // B = $1,500,000
    private static final int CAPACIDAD_ALMACEN = 5_000;          // A = 5,000 unidades espacio
    private static final int HORAS_PERSONAL_MAXIMO = 160;        // Horas totales disponibles
    private static final double HORAS_POR_UNIDAD = 0.1;          // Horas por unidad vendida
    private static final double COEFICIENTE_ALMACENAMIENTO = 0.05; // Coeficiente cuadrático

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                maximizarUtilidad(factory),
                restriccionPresupuesto(factory),
                restriccionAlmacen(factory),
                restriccionDemandaMinima(factory),
                restriccionHorasPersonal(factory),
                restriccionDescuentoIncoherente(factory)
        };
    }

    // ==================== FUNCIÓN OBJETIVO (SOFT) ====================

    /**
     * Función Objetivo: Maximizar Utilidad (NO LINEAL)
     * 
     * Para cada producto i:
     *   Ingresos_i = Precio_i * (1 - descuento_i) * MIN(demanda_i, cantActual_i + cantAPedir_i)
     *   Costos_i   = costoCompra_i * cantAPedir_i
     *   CostoAlm_i = 0.05 * (cantActual_i + cantAPedir_i)²   ← Componente NO LINEAL (cuadrático)
     *   
     *   Utilidad_i = Ingresos_i - Costos_i - CostoAlm_i
     * 
     * Maximizar: Σ Utilidad_i
     */
    Constraint maximizarUtilidad(ConstraintFactory factory) {
        return factory.forEach(PropuestaPedido.class)
                .impact(HardSoftScore.ONE_SOFT,
                        propuesta -> {
                            double utilidad = propuesta.calcularUtilidad();
                            // Convertir a entero (Timefold trabaja con scores enteros)
                            // Multiplicamos por 100 para mantener 2 decimales de precisión
                            // .impact soporta tanto valores positivos (ganancia) como negativos (pérdida)
                            return (int) (utilidad * 100);
                        })
                .asConstraint("Maximizar Utilidad");
    }

    // ==================== RESTRICCIONES HARD ====================

    /**
     * R1 - Restricción de Presupuesto:
     * Σ (costoCompra_i * cantidadAPedir_i) ≤ B (1,500,000)
     * 
     * Penaliza proporcionalmente al exceso sobre el presupuesto.
     */
    Constraint restriccionPresupuesto(ConstraintFactory factory) {
        return factory.forEach(PropuestaPedido.class)
                .groupBy(ConstraintCollectors.sum(
                        propuesta -> {
                            double costo = propuesta.getCostoCompraProducto();
                            int aPedir = propuesta.getCantidadAPedir() != null ? propuesta.getCantidadAPedir() : 0;
                            return (int) (costo * aPedir);
                        }))
                .filter(totalCosto -> totalCosto > PRESUPUESTO_MAXIMO)
                .penalize(HardSoftScore.ONE_HARD,
                        totalCosto -> totalCosto - PRESUPUESTO_MAXIMO)
                .asConstraint("R1 - Presupuesto");
    }

    /**
     * R2 - Restricción de Almacén:
     * Σ (espacioUnidad_i * (cantActual_i + cantAPedir_i)) ≤ A (5,000)
     * 
     * Penaliza proporcionalmente al exceso de espacio.
     */
    Constraint restriccionAlmacen(ConstraintFactory factory) {
        return factory.forEach(PropuestaPedido.class)
                .groupBy(ConstraintCollectors.sum(
                        propuesta -> {
                            int actual = propuesta.getCantidadActual();
                            int aPedir = propuesta.getCantidadAPedir() != null ? propuesta.getCantidadAPedir() : 0;
                            double espacio = propuesta.getEspacioUnidad() != null ? propuesta.getEspacioUnidad() : 0.0;
                            return (int) (espacio * (actual + aPedir));
                        }))
                .filter(totalEspacio -> totalEspacio > CAPACIDAD_ALMACEN)
                .penalize(HardSoftScore.ONE_HARD,
                        totalEspacio -> totalEspacio - CAPACIDAD_ALMACEN)
                .asConstraint("R2 - Almacen");
    }

    /**
     * R3 - Restricción de Demanda Mínima:
     * Para cada producto i: (cantActual_i + cantAPedir_i) ≥ demandaEstimada_i
     * 
     * Penaliza por cada unidad que falte para cubrir la demanda.
     */
    Constraint restriccionDemandaMinima(ConstraintFactory factory) {
        return factory.forEach(PropuestaPedido.class)
                .filter(propuesta -> {
                    int disponible = propuesta.getCantidadActual() + propuesta.getCantidadAPedir();
                    return disponible < propuesta.getDemandaEstimada();
                })
                .penalize(HardSoftScore.ONE_HARD,
                        propuesta -> {
                            int actual = propuesta.getCantidadActual();
                            int aPedir = propuesta.getCantidadAPedir() != null ? propuesta.getCantidadAPedir() : 0;
                            int demanda = propuesta.getDemandaEstimada() != null ? propuesta.getDemandaEstimada() : 0;
                            return demanda - (actual + aPedir);
                        })
                .asConstraint("R3 - Demanda Minima");
    }

    /**
     * R4 - Restricción de Horas de Personal:
     * Σ (0.1 * MIN(demanda_i, cantActual_i + cantAPedir_i)) ≤ 160 horas
     * 
     * Cada unidad vendida requiere 0.1 horas de personal.
     * Equivalente: Σ MIN(demanda_i, cantActual_i + cantAPedir_i) ≤ 1600
     * (multiplicamos ambos lados por 10 para evitar decimales)
     */
    Constraint restriccionHorasPersonal(ConstraintFactory factory) {
        return factory.forEach(PropuestaPedido.class)
                .groupBy(ConstraintCollectors.sum(
                        propuesta -> {
                            int actual = propuesta.getCantidadActual();
                            int aPedir = propuesta.getCantidadAPedir() != null ? propuesta.getCantidadAPedir() : 0;
                            int demanda = propuesta.getDemandaEstimada() != null ? propuesta.getDemandaEstimada() : 0;
                            return Math.min(demanda, actual + aPedir);
                        }))
                .filter(totalVendidas -> totalVendidas > (int) (HORAS_PERSONAL_MAXIMO / HORAS_POR_UNIDAD))
                .penalize(HardSoftScore.ONE_HARD,
                        totalVendidas -> totalVendidas - (int) (HORAS_PERSONAL_MAXIMO / HORAS_POR_UNIDAD))
                .asConstraint("R4 - Horas Personal");
    }

    /**
     * R5 - Restricción de Descuento Incoherente:
     * Si cantidadActual_i = 0, entonces descuentoProximidad_i debe ser 0.
     * 
     * No tiene sentido aplicar descuento por proximidad a vencimiento
     * a un producto que no existe físicamente en inventario.
     * Penaliza con 1000 puntos por cada violación.
     */
    Constraint restriccionDescuentoIncoherente(ConstraintFactory factory) {
        return factory.forEach(PropuestaPedido.class)
                .filter(propuesta -> propuesta.getCantidadActual() == 0
                        && propuesta.getDescuentoProximidad() != null
                        && propuesta.getDescuentoProximidad() > 0.0)
                .penalize(HardSoftScore.ONE_HARD,
                        propuesta -> 1000) // Penalización fija severa
                .asConstraint("R5 - Descuento Incoherente");
    }
}
