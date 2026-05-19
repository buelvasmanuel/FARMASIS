package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.PlanOptimizacion;
import com.admin.adminlfarma_mini.entity.PropuestaPedido;
import com.admin.adminlfarma_mini.service.OptimizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador REST para el endpoint de optimización de inventario.
 * Expone el resultado del modelo matemático de programación no lineal
 * que calcula las cantidades óptimas de pedido y descuentos.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class OptimizacionController {

    private final OptimizacionService optimizacionService;

    /**
     * GET /api/optimizar
     * 
     * Ejecuta el solver de optimización y retorna un JSON con:
     * - Score de la solución (hard/soft)
     * - Lista de propuestas optimizadas por producto
     * - Resumen financiero total
     * 
     * @return JSON con las cantidades óptimas que la farmacia debe pedir
     */
    @GetMapping("/api/optimizar")
    public ResponseEntity<?> optimizar() {
        try {
            log.info("Solicitud de optimización recibida en /api/optimizar");

            PlanOptimizacion solucion = optimizacionService.optimizar();

            // Construir respuesta JSON estructurada
            Map<String, Object> response = new HashMap<>();
            response.put("status", "OK");
            response.put("score", solucion.getScore() != null ? solucion.getScore().toString() : "N/A");
            response.put("hardScore", solucion.getScore() != null ? solucion.getScore().hardScore() : 0);
            response.put("softScore", solucion.getScore() != null ? solucion.getScore().softScore() : 0);

            // Mapear cada propuesta a un objeto legible
            List<Map<String, Object>> propuestasJson = solucion.getPropuestas().stream()
                    .map(this::mapearPropuesta)
                    .collect(Collectors.toList());

            response.put("propuestas", propuestasJson);

            // Calcular resumen financiero
            response.put("resumen", calcularResumen(solucion.getPropuestas()));

            log.info("Optimización completada exitosamente. Score: {}", solucion.getScore());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error durante la optimización: {}", e.getMessage());
            e.printStackTrace();
            if (e.getCause() != null) {
                log.error("Causa raíz:");
                e.getCause().printStackTrace();
            }
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("mensaje", "Error al ejecutar la optimización: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Convierte una PropuestaPedido en un mapa JSON legible.
     */
    private Map<String, Object> mapearPropuesta(PropuestaPedido p) {
        Map<String, Object> propuesta = new HashMap<>();
        propuesta.put("productoNombre", p.getNombreProducto());
        propuesta.put("productoCodigo", p.getProducto() != null ? p.getProducto().getCodigo() : "N/A");
        propuesta.put("precioBase", p.getPrecioBase());
        propuesta.put("costoCompra", p.getCostoCompraProducto());
        propuesta.put("cantidadActual", p.getCantidadActual());
        propuesta.put("demandaEstimada", p.getDemandaEstimada());
        propuesta.put("espacioUnidad", p.getEspacioUnidad());

        // Variables optimizadas por Timefold
        propuesta.put("cantidadAPedir", p.getCantidadAPedir());
        propuesta.put("descuentoProximidad", p.getDescuentoProximidad());
        propuesta.put("descuentoPorcentaje",
                p.getDescuentoProximidad() != null
                        ? String.format("%.0f%%", p.getDescuentoProximidad() * 100)
                        : "0%");

        // Métricas calculadas
        propuesta.put("utilidadEstimada", Math.round(p.calcularUtilidad() * 100.0) / 100.0);
        propuesta.put("costoTotalPedido",
                Math.round(p.getCostoCompraProducto() * p.getCantidadAPedir() * 100.0) / 100.0);
        propuesta.put("stockResultante", p.getCantidadActual() + p.getCantidadAPedir());

        return propuesta;
    }

    /**
     * Calcula el resumen financiero global de la solución.
     */
    private Map<String, Object> calcularResumen(List<PropuestaPedido> propuestas) {
        Map<String, Object> resumen = new HashMap<>();

        double utilidadTotal = propuestas.stream()
                .mapToDouble(PropuestaPedido::calcularUtilidad)
                .sum();

        double costoTotalPedidos = propuestas.stream()
                .mapToDouble(p -> p.getCostoCompraProducto() * p.getCantidadAPedir())
                .sum();

        double espacioTotalUsado = propuestas.stream()
                .mapToDouble(p -> p.getEspacioUnidad() * (p.getCantidadActual() + p.getCantidadAPedir()))
                .sum();

        double horasTotalesRequeridas = propuestas.stream()
                .mapToDouble(p -> 0.1 * Math.min(
                        p.getDemandaEstimada(),
                        p.getCantidadActual() + p.getCantidadAPedir()))
                .sum();

        int totalUnidadesAPedir = propuestas.stream()
                .mapToInt(PropuestaPedido::getCantidadAPedir)
                .sum();

        resumen.put("utilidadTotalEstimada", Math.round(utilidadTotal * 100.0) / 100.0);
        resumen.put("costoTotalPedidos", Math.round(costoTotalPedidos * 100.0) / 100.0);
        resumen.put("presupuestoMaximo", 1_500_000);
        resumen.put("presupuestoUsadoPorcentaje",
                String.format("%.1f%%", (costoTotalPedidos / 1_500_000) * 100));
        resumen.put("espacioTotalUsado", Math.round(espacioTotalUsado * 100.0) / 100.0);
        resumen.put("capacidadAlmacen", 5_000);
        resumen.put("horasTotalesRequeridas", Math.round(horasTotalesRequeridas * 100.0) / 100.0);
        resumen.put("horasDisponibles", 160);
        resumen.put("totalUnidadesAPedir", totalUnidadesAPedir);

        return resumen;
    }
}
