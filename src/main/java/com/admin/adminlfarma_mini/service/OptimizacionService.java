package com.admin.adminlfarma_mini.service;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import com.admin.adminlfarma_mini.entity.PlanOptimizacion;
import com.admin.adminlfarma_mini.entity.Producto;
import com.admin.adminlfarma_mini.entity.PropuestaPedido;
import com.admin.adminlfarma_mini.entity.ResultadoOptimizacion;
import com.admin.adminlfarma_mini.repository.ProductoRepository;
import com.admin.adminlfarma_mini.repository.ResultadoOptimizacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Servicio de optimización de inventario farmacéutico.
 * Utiliza Timefold Solver para encontrar las cantidades óptimas de pedido
 * y descuentos que maximizan la utilidad total.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizacionService {

    private final SolverManager<PlanOptimizacion, Long> solverManager;
    private final ProductoRepository productoRepository;
    private final ResultadoOptimizacionRepository resultadoRepository;

    /**
     * Ejecuta el solver de optimización.
     * Carga productos reales de la BD (o genera datos mock si no hay suficientes)
     * y retorna la solución óptima.
     */
    public PlanOptimizacion optimizar() throws InterruptedException, ExecutionException {
        log.info("=== INICIANDO OPTIMIZACIÓN DE INVENTARIO ===");

        // Crear el problema con datos reales o mock
        PlanOptimizacion problema = crearProblema();

        log.info("Problema creado con {} propuestas de pedido", problema.getPropuestas().size());

        // Enviar al solver y esperar la mejor solución
        SolverJob<PlanOptimizacion, Long> solverJob = solverManager.solve(1L, problema);
        PlanOptimizacion solucion = solverJob.getFinalBestSolution();

        log.info("=== OPTIMIZACIÓN COMPLETADA ===");
        log.info("Score final: {}", solucion.getScore());

        // Log detallado de cada propuesta
        for (PropuestaPedido p : solucion.getPropuestas()) {
            log.info("Producto: {} | Cantidad a pedir: {} | Descuento: {}% | Utilidad: ${:.2f}",
                    p.getNombreProducto(),
                    p.getCantidadAPedir(),
                    p.getDescuentoProximidad() != null ? p.getDescuentoProximidad() * 100 : 0,
                    p.calcularUtilidad());
        }

        return solucion;
    }

    /**
     * Crea el problema de optimización con las propuestas de pedido.
     * Utiliza estrictamente productos reales de MongoDB bajo stock.
     */
    private PlanOptimizacion crearProblema() {
        List<PropuestaPedido> propuestas = new ArrayList<>();
        // PRE-FILTRO: Solo productos cuyo stock <= su stockMinimo individual
        List<Producto> productosReales = productoRepository.findProductosBajoStock();

        log.info("Productos bajo stock encontrados: {}", productosReales.size());

        long idCounter = 1L;
        for (Producto prod : productosReales) {
            // Asegurar que campos críticos del producto no sean nulos
            if (prod.getPrecio() == null) prod.setPrecio(0.0);
            if (prod.getCostoCompra() == null) prod.setCostoCompra(0.0);
            if (prod.getCantidad() == null) prod.setCantidad(0);
            
            PropuestaPedido propuesta = new PropuestaPedido();
            propuesta.setId(idCounter++);
            propuesta.setProducto(prod);
            propuesta.setDemandaEstimada(calcularDemandaEstimada(prod));
            propuesta.setEspacioUnidad(calcularEspacioUnidad(prod));
            
            // Inicializar variables de planificación para evitar nulos iniciales
            propuesta.setCantidadAPedir(0);
            propuesta.setDescuentoProximidad(0.0);
            
            propuestas.add(propuesta);
        }

        return new PlanOptimizacion(propuestas);
    }

    /**
     * Estima la demanda basándose en el stock actual del producto.
     * Heurística simple: demanda ≈ 1.5x la cantidad actual (mínimo 10).
     */
    private Integer calcularDemandaEstimada(Producto producto) {
        int cantidadActual = (producto.getCantidad() != null) ? producto.getCantidad() : 0;
        return Math.max(10, (int) (cantidadActual * 1.5));
    }

    /**
     * Estima el espacio por unidad según la categoría/presentación del producto.
     */
    private Double calcularEspacioUnidad(Producto producto) {
        // Valor por defecto razonable para productos farmacéuticos
        return 1.2;
    }

    /**
     * Guarda (sobrescribe) el resultado de la última optimización en BD.
     */
    public ResultadoOptimizacion guardarResultado(String score, Integer hardScore, Integer softScore,
                                                   List<Map<String, Object>> propuestas,
                                                   Map<String, Object> resumen) {
        resultadoRepository.deleteAll();
        ResultadoOptimizacion resultado = new ResultadoOptimizacion();
        resultado.setScore(score);
        resultado.setHardScore(hardScore);
        resultado.setSoftScore(softScore);
        resultado.setPropuestas(propuestas);
        resultado.setResumen(resumen);
        resultado.setFechaCalculo(LocalDateTime.now());
        return resultadoRepository.save(resultado);
    }

    /**
     * Obtiene el último resultado de optimización guardado (borrador).
     */
    public Optional<ResultadoOptimizacion> obtenerUltimoResultado() {
        return resultadoRepository.findTopByOrderByFechaCalculoDesc();
    }
}
