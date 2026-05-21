package com.admin.adminlfarma_mini.entity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Entidad de Planificación para el modelo de optimización de inventario.
 * Representa una propuesta de pedido para un producto específico.
 * Timefold optimizará las variables cantidadAPedir y descuentoProximidad.
 */
@PlanningEntity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropuestaPedido {

    /** Identificador único de la propuesta */
    private Long id;

    /** Producto de la BD asociado a esta propuesta */
    private Producto producto;

    /** Demanda estimada de este producto (unidades) */
    private Integer demandaEstimada;

    /** Espacio que ocupa una unidad en almacén (m³ o unidades de espacio) */
    private Double espacioUnidad;

    // ==================== VARIABLES DE PLANIFICACIÓN ====================

    /**
     * Variable de planificación: Cantidad a pedir al proveedor.
     * Rango: 0 a 500 unidades.
     * El solver determinará el valor óptimo.
     */
    @PlanningVariable(valueRangeProviderRefs = "cantidadRange")
    private Integer cantidadAPedir;

    /**
     * Variable de planificación: Descuento por proximidad a vencimiento.
     * Rango: 0.0 a 0.5 con saltos de 0.05.
     * El solver determinará el descuento óptimo a aplicar.
     */
    @PlanningVariable(valueRangeProviderRefs = "descuentoRange")
    private Double descuentoProximidad;

    // ==================== MÉTODOS DE CONVENIENCIA ====================

    /** Obtiene la cantidad actual del producto en inventario (null-safe) */
    public int getCantidadActual() {
        return (producto != null && producto.getCantidad() != null)
                ? producto.getCantidad() : 0;
    }

    /** Obtiene el precio base del producto (null-safe) */
    public double getPrecioBase() {
        return (producto != null && producto.getPrecio() != null)
                ? producto.getPrecio() : 0.0;
    }

    /** Obtiene el costo de compra del producto (null-safe) */
    public double getCostoCompraProducto() {
        return (producto != null && producto.getCostoCompra() != null)
                ? producto.getCostoCompra() : 0.0;
    }

    /** Obtiene el nombre del producto (null-safe) */
    public String getNombreProducto() {
        return (producto != null && producto.getNombre() != null)
                ? producto.getNombre() : "Desconocido";
    }

    /**
     * Calcula el descuento aplicable garantizando al menos un 1% de ganancia mínima.
     */
    public double getDescuentoAplicable() {
        if (descuentoProximidad == null) return 0.0;
        double precio = getPrecioBase();
        double costo = getCostoCompraProducto();
        if (precio == 0.0 || precio <= costo) return 0.0; // Evitar descuentos si ya no hay margen

        // maxDescuento en decimal (ej. 0.20 para 20%)
        double maxDescuento = (precio - costo) / precio;

        // Si el descuento sugerido es mayor o igual al máximo, lo reducimos para garantizar 1% (0.01) de ganancia
        if (descuentoProximidad >= maxDescuento) {
            return Math.max(0.0, maxDescuento - 0.01);
        }
        return descuentoProximidad;
    }

    /**
     * Calcula la utilidad estimada de la propuesta.
     * Utilidad = (PrecioConDescuento - CostoCompra) * StockFinal
     * Esta utilidad jamás debe ser negativa.
     */
    public double calcularUtilidad() {
        if (cantidadAPedir == null) return 0.0;

        int cantActual = getCantidadActual();
        double precio = getPrecioBase();
        double costoCompra = getCostoCompraProducto();
        
        double descuentoFinal = getDescuentoAplicable();
        double precioConDescuento = precio * (1.0 - descuentoFinal);
        
        int stockFinal = cantActual + cantidadAPedir;
        
        double utilidad = (precioConDescuento - costoCompra) * stockFinal;

        return Math.max(0.0, utilidad);
    }
}
