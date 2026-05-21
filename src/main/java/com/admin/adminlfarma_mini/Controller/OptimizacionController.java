package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.PlanOptimizacion;
import com.admin.adminlfarma_mini.entity.PropuestaPedido;
import com.admin.adminlfarma_mini.entity.ResultadoOptimizacion;
import com.admin.adminlfarma_mini.service.OptimizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OptimizacionController {

    private final OptimizacionService optimizacionService;

    @GetMapping("/api/optimizar")
    public ResponseEntity<?> optimizar() {
        try {
            log.info("Solicitud de optimización recibida en /api/optimizar");
            PlanOptimizacion solucion = optimizacionService.optimizar();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "OK");
            response.put("score", solucion.getScore() != null ? solucion.getScore().toString() : "N/A");
            response.put("hardScore", solucion.getScore() != null ? solucion.getScore().hardScore() : 0);
            response.put("softScore", solucion.getScore() != null ? solucion.getScore().softScore() : 0);

            List<Map<String, Object>> propuestasJson = solucion.getPropuestas().stream()
                    .filter(p -> p.getCantidadAPedir() != null && (p.getCantidadAPedir() > 0 || p.getDescuentoAplicable() > 0.0))
                    .map(this::mapearPropuesta)
                    .collect(Collectors.toList());
            response.put("propuestas", propuestasJson);

            Map<String, Object> resumen = calcularResumen(solucion.getPropuestas());
            response.put("resumen", resumen);

            // Persistir resultado (sobrescribe el anterior)
            optimizacionService.guardarResultado(
                    response.get("score").toString(),
                    (Integer) response.get("hardScore"),
                    (Integer) response.get("softScore"),
                    propuestasJson, resumen);

            log.info("Optimización completada y guardada. Score: {}", solucion.getScore());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error durante la optimización: {}", e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("mensaje", "Error al ejecutar la optimización: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/api/optimizacion/ultimo")
    public ResponseEntity<?> obtenerUltimo() {
        Optional<ResultadoOptimizacion> resultado = optimizacionService.obtenerUltimoResultado();
        if (resultado.isPresent()) {
            ResultadoOptimizacion r = resultado.get();
            
            List<Map<String, Object>> propuestasFiltradas = r.getPropuestas().stream()
                    .filter(map -> {
                        Object cant = map.get("cantidadAPedir");
                        Object desc = map.get("descuentoProximidad");
                        int c = cant instanceof Number ? ((Number) cant).intValue() : 0;
                        double d = desc instanceof Number ? ((Number) desc).doubleValue() : 0.0;
                        return c > 0 || d > 0.0;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "OK");
            response.put("score", r.getScore());
            response.put("hardScore", r.getHardScore());
            response.put("softScore", r.getSoftScore());
            response.put("propuestas", propuestasFiltradas);
            response.put("resumen", r.getResumen());
            response.put("fechaCalculo", r.getFechaCalculo().toString());
            return ResponseEntity.ok(response);
        }
        Map<String, Object> empty = new HashMap<>();
        empty.put("status", "EMPTY");
        return ResponseEntity.ok(empty);
    }

    private Map<String, Object> mapearPropuesta(PropuestaPedido p) {
        Map<String, Object> propuesta = new HashMap<>();
        propuesta.put("productoNombre", p.getNombreProducto());
        propuesta.put("productoId", p.getProducto() != null ? p.getProducto().getId() : null);
        propuesta.put("productoCodigo", p.getProducto() != null ? p.getProducto().getCodigo() : "N/A");
        propuesta.put("productoCategoria", p.getProducto() != null ? p.getProducto().getCategoria() : "Otros");
        propuesta.put("precioBase", p.getPrecioBase());
        propuesta.put("precioOriginal", p.getProducto() != null ? p.getProducto().getPrecioOriginal() : null);
        propuesta.put("costoCompra", p.getCostoCompraProducto());
        propuesta.put("cantidadActual", p.getCantidadActual());
        propuesta.put("demandaEstimada", p.getDemandaEstimada());
        propuesta.put("espacioUnidad", p.getEspacioUnidad());
        propuesta.put("cantidadAPedir", p.getCantidadAPedir());
        propuesta.put("descuentoProximidad", p.getDescuentoAplicable());
        propuesta.put("descuentoPorcentaje", String.format("%.0f%%", p.getDescuentoAplicable() * 100));
        propuesta.put("utilidadEstimada", Math.round(p.calcularUtilidad() * 100.0) / 100.0);
        propuesta.put("costoTotalPedido",
                Math.round(p.getCostoCompraProducto() * p.getCantidadAPedir() * 100.0) / 100.0);
        propuesta.put("stockResultante", p.getCantidadActual() + p.getCantidadAPedir());
        return propuesta;
    }

    private Map<String, Object> calcularResumen(List<PropuestaPedido> propuestas) {
        Map<String, Object> resumen = new HashMap<>();
        double utilidadTotal = propuestas.stream().mapToDouble(PropuestaPedido::calcularUtilidad).sum();
        double costoTotalPedidos = propuestas.stream()
                .mapToDouble(p -> p.getCostoCompraProducto() * p.getCantidadAPedir()).sum();
        double espacioTotalUsado = propuestas.stream()
                .mapToDouble(p -> p.getEspacioUnidad() * (p.getCantidadActual() + p.getCantidadAPedir())).sum();
        double horasTotalesRequeridas = propuestas.stream()
                .mapToDouble(p -> 0.1 * Math.min(p.getDemandaEstimada(), p.getCantidadActual() + p.getCantidadAPedir())).sum();
        int totalUnidadesAPedir = propuestas.stream().mapToInt(PropuestaPedido::getCantidadAPedir).sum();

        resumen.put("utilidadTotalEstimada", Math.round(utilidadTotal * 100.0) / 100.0);
        resumen.put("costoTotalPedidos", Math.round(costoTotalPedidos * 100.0) / 100.0);
        resumen.put("presupuestoMaximo", 1_500_000);
        resumen.put("presupuestoUsadoPorcentaje", String.format("%.1f%%", (costoTotalPedidos / 1_500_000) * 100));
        resumen.put("espacioTotalUsado", Math.round(espacioTotalUsado * 100.0) / 100.0);
        resumen.put("capacidadAlmacen", 5_000);
        resumen.put("horasTotalesRequeridas", Math.round(horasTotalesRequeridas * 100.0) / 100.0);
        resumen.put("horasDisponibles", 160);
        resumen.put("totalUnidadesAPedir", totalUnidadesAPedir);
        return resumen;
    }
}
