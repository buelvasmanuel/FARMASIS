package com.admin.adminlfarma_mini.service;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import com.admin.adminlfarma_mini.entity.PlanOptimizacion;
import com.admin.adminlfarma_mini.entity.Producto;
import com.admin.adminlfarma_mini.entity.PropuestaPedido;
import com.admin.adminlfarma_mini.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
     * Crea el problema de optimización con 5 propuestas de pedido.
     * Intenta usar productos reales de MongoDB; si no hay suficientes,
     * completa con datos mock representativos de una farmacia.
     */
    private PlanOptimizacion crearProblema() {
        List<PropuestaPedido> propuestas = new ArrayList<>();
        List<Producto> productosReales = productoRepository.findAll();

        log.info("Productos encontrados en BD: {}", productosReales.size());

        // Usar hasta 5 productos reales
        long idCounter = 1L;
        for (int i = 0; i < Math.min(5, productosReales.size()); i++) {
            Producto prod = productosReales.get(i);
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

        // Si no hay suficientes productos reales, completar con datos mock
        if (propuestas.size() < 5) {
            log.info("Completando con {} productos mock", 5 - propuestas.size());
            List<PropuestaPedido> mocks = generarProductosMock(idCounter, 5 - propuestas.size());
            propuestas.addAll(mocks);
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
     * Genera productos mock representativos de una farmacia.
     * Estos datos simulan productos típicos con precios y costos realistas.
     */
    private List<PropuestaPedido> generarProductosMock(long startId, int cantidad) {
        // Datos mock de productos farmacéuticos típicos
        String[][] datosMock = {
                {"Acetaminofén 500mg",   "8500",  "4200",  "45"},
                {"Ibuprofeno 400mg",     "12000", "6500",  "30"},
                {"Amoxicilina 500mg",    "18500", "9800",  "20"},
                {"Omeprazol 20mg",       "15000", "7500",  "35"},
                {"Loratadina 10mg",      "9500",  "4800",  "25"},
        };

        int[] demandasMock = {60, 50, 40, 55, 35};
        double[] espaciosMock = {0.8, 1.0, 1.2, 0.9, 0.7};

        List<PropuestaPedido> mocks = new ArrayList<>();

        for (int i = 0; i < cantidad && i < datosMock.length; i++) {
            // Crear producto mock
            Producto productoMock = new Producto();
            productoMock.setId("MOCK-" + (startId + i));
            productoMock.setCodigo(String.format("%04d", 9000 + i));
            productoMock.setNombre(datosMock[i][0]);
            productoMock.setPrecio(Double.parseDouble(datosMock[i][1]));
            productoMock.setCostoCompra(Double.parseDouble(datosMock[i][2]));
            productoMock.setCantidad(Integer.parseInt(datosMock[i][3]));
            productoMock.setActivo(true);

            // Crear propuesta
            PropuestaPedido propuesta = new PropuestaPedido();
            propuesta.setId(startId + i);
            propuesta.setProducto(productoMock);
            propuesta.setDemandaEstimada(demandasMock[i]);
            propuesta.setEspacioUnidad(espaciosMock[i]);
            
            // Inicializar variables de planificación
            propuesta.setCantidadAPedir(0);
            propuesta.setDescuentoProximidad(0.0);
            
            mocks.add(propuesta);
        }

        return mocks;
    }
}
