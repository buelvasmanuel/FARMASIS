package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.DTO.FacturaRequestDTO;
import com.admin.adminlfarma_mini.entity.Factura;
import com.admin.adminlfarma_mini.entity.Producto;
import com.admin.adminlfarma_mini.service.ClienteService;
import com.admin.adminlfarma_mini.service.FacturaService;
import com.admin.adminlfarma_mini.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;
    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final com.admin.adminlfarma_mini.service.ConfiguracionService configuracionService;

    @GetMapping
    public String listarVentas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String factura,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());

        boolean tieneFiltreo = (fechaDesde != null && !fechaDesde.isEmpty())
                || (fechaHasta != null && !fechaHasta.isEmpty())
                || (metodoPago != null && !metodoPago.isEmpty())
                || (factura != null && !factura.isEmpty());

        Page<Factura> ventasPage;
        if (tieneFiltreo) {
            ventasPage = facturaService.buscarFacturas(fechaDesde, fechaHasta, metodoPago, factura, pageable);
        } else {
            ventasPage = facturaService.listarFacturas(pageable);
        }

        model.addAttribute("ventas", ventasPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ventasPage.getTotalPages());
        model.addAttribute("totalItems", ventasPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("ventasDelDia", facturaService.getVentasDelDia());
        model.addAttribute("cantidadVentasDelDia", facturaService.getCantidadVentasDelDia());
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);
        model.addAttribute("metodoPago", metodoPago);
        model.addAttribute("factura", factura);

        return "ventas";
    }

    @GetMapping("/exportar")
    @ResponseBody
    public ResponseEntity<byte[]> exportarCSV(
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String factura) {

        List<Factura> facturas = facturaService.exportarFacturas(fechaDesde, fechaHasta, metodoPago, factura);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        StringBuilder csv = new StringBuilder();
        csv.append("# Factura,Fecha,Cliente,Subtotal,IVA,Total,Método de Pago,Observaciones\n");
        for (Factura f : facturas) {
            csv.append(String.format("\"%s\",\"%s\",\"%s\",%.2f,%.2f,%.2f,\"%s\",\"%s\"\n",
                    f.getNumeroFactura(),
                    f.getFecha() != null ? f.getFecha().format(fmt) : "",
                    f.getClienteNombre() != null ? f.getClienteNombre() : "",
                    f.getSubtotal() != null ? f.getSubtotal() : 0,
                    f.getIva() != null ? f.getIva() : 0,
                    f.getTotal() != null ? f.getTotal() : 0,
                    f.getMetodoPago() != null ? f.getMetodoPago() : "",
                    f.getObservaciones() != null ? f.getObservaciones().replace("\"", "'") : ""));
        }

        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        // Add BOM for Excel UTF-8 compatibility
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(csvBytes, 0, result, bom.length, csvBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ventas_lfarma.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(result);
    }

    @GetMapping("/nueva")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'EMPLEADO')")
    public String nuevaVenta(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String search,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        Page<Producto> productosPage;

        if (search != null && !search.isEmpty()) {
            if (categoria != null && !categoria.isEmpty()) {
                productosPage = productoService.getProductosParaPOSConBusquedaYCategoria(search, categoria, pageable);
            } else {
                productosPage = productoService.getProductosParaPOSConBusqueda(search, pageable);
            }
        } else {
            if (categoria != null && !categoria.isEmpty()) {
                productosPage = productoService.getProductosParaPOSPorCategoria(categoria, pageable);
            } else {
                productosPage = productoService.getProductosParaPOS(pageable);
            }
        }

        model.addAttribute("productos", productosPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productosPage.getTotalPages());
        model.addAttribute("totalItems", productosPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("categoriaActual", categoria);
        model.addAttribute("search", search);
        model.addAttribute("clientes", clienteService.listarTodos());
        model.addAttribute("consumidorFinal", clienteService.getConsumidorFinal());

        List<String> categorias = productoService.obtenerCategorias();
        model.addAttribute("categorias", categorias);

        return "crear-venta";
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarVenta(@RequestBody FacturaRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Factura factura = facturaService.crearFactura(request);
            response.put("success", true);
            response.put("message", "Venta registrada exitosamente");
            response.put("facturaId", factura.getId());
            response.put("numeroFactura", factura.getNumeroFactura());
            response.put("total", factura.getTotal());
            response.put("cliente", factura.getClienteNombre());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/factura")
    public String verFactura(@PathVariable String id, Model model) {
        Factura factura = facturaService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));
        model.addAttribute("factura", factura);
        model.addAttribute("config", configuracionService.getConfiguracion());
        return "factura-ticket";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Factura> obtenerFactura(@PathVariable String id) {
        return facturaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}