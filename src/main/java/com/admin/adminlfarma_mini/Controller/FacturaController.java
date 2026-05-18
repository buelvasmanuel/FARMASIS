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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Controller
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;
    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final com.admin.adminlfarma_mini.service.ConfiguracionService configuracionService;
    private final com.admin.adminlfarma_mini.service.UsuarioService usuarioService;

    @GetMapping
    public String listarVentas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String factura,
            @RequestParam(required = false) String vendedor,
            Model model,
            java.security.Principal principal) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());

        List<String> allowedVendedores = new java.util.ArrayList<>();
        boolean isOwner = false;
        if (principal != null) {
            String loggedUserEmail = principal.getName();
            com.admin.adminlfarma_mini.entity.Usuario loggedUser = usuarioService.buscarPorUsername(loggedUserEmail).orElse(null);
            if (loggedUser != null) {
                if ("ROLE_OWNER".equals(loggedUser.getRol())) {
                    isOwner = true;
                    if (vendedor != null && !vendedor.trim().isEmpty()) {
                        allowedVendedores.add(vendedor.trim());
                    }
                } else if ("ROLE_ADMIN".equals(loggedUser.getRol())) {
                    List<com.admin.adminlfarma_mini.entity.Usuario> allUsers = usuarioService.listarTodosIncluyendoInactivos();
                    for (com.admin.adminlfarma_mini.entity.Usuario u : allUsers) {
                        if (!"ROLE_OWNER".equals(u.getRol())) {
                            allowedVendedores.add(u.getUsername());
                        }
                    }
                } else {
                    allowedVendedores.add(loggedUserEmail);
                }
            }
        }

        if (!isOwner && allowedVendedores.isEmpty()) {
            allowedVendedores.add("dummy_no_match");
        }

        Page<Factura> ventasPage = facturaService.buscarFacturas(fechaDesde, fechaHasta, metodoPago, factura, allowedVendedores, pageable);

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
        model.addAttribute("vendedor", vendedor);

        return "ventas";
    }

    @GetMapping("/exportar")
    @ResponseBody
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String factura,
            @RequestParam(required = false) String vendedor,
            java.security.Principal principal) throws IOException {

        List<String> allowedVendedores = new java.util.ArrayList<>();
        boolean isOwner = false;
        if (principal != null) {
            String loggedUserEmail = principal.getName();
            com.admin.adminlfarma_mini.entity.Usuario loggedUser = usuarioService.buscarPorUsername(loggedUserEmail).orElse(null);
            if (loggedUser != null) {
                if ("ROLE_OWNER".equals(loggedUser.getRol())) {
                    isOwner = true;
                    if (vendedor != null && !vendedor.trim().isEmpty()) {
                        allowedVendedores.add(vendedor.trim());
                    }
                } else if ("ROLE_ADMIN".equals(loggedUser.getRol())) {
                    List<com.admin.adminlfarma_mini.entity.Usuario> allUsers = usuarioService.listarTodosIncluyendoInactivos();
                    for (com.admin.adminlfarma_mini.entity.Usuario u : allUsers) {
                        if (!"ROLE_OWNER".equals(u.getRol())) {
                            allowedVendedores.add(u.getUsername());
                        }
                    }
                } else {
                    allowedVendedores.add(loggedUserEmail);
                }
            }
        }

        if (!isOwner && allowedVendedores.isEmpty()) {
            allowedVendedores.add("dummy_no_match");
        }

        List<Factura> facturas = facturaService.exportarFacturas(fechaDesde, fechaHasta, metodoPago, factura, allowedVendedores);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Ventas");

            // Estilo de encabezado: verde oscuro con texto blanco
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Encabezados
            Row header = sheet.createRow(0);
            String[] cols = {"Factura", "Fecha", "Cliente", "Subtotal", "IVA", "Total", "Método de Pago", "Observaciones", "Vendedor"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Datos
            int rowNum = 1;
            for (Factura f : facturas) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(f.getNumeroFactura() != null ? f.getNumeroFactura() : "");
                row.createCell(1).setCellValue(f.getFecha() != null ? f.getFecha().format(fmt) : "");
                row.createCell(2).setCellValue(f.getClienteNombre() != null ? f.getClienteNombre() : "");
                row.createCell(3).setCellValue(f.getSubtotal() != null ? f.getSubtotal() : 0.0);
                row.createCell(4).setCellValue(f.getIva() != null ? f.getIva() : 0.0);
                row.createCell(5).setCellValue(f.getTotal() != null ? f.getTotal() : 0.0);
                row.createCell(6).setCellValue(f.getMetodoPago() != null ? f.getMetodoPago() : "");
                row.createCell(7).setCellValue(f.getObservaciones() != null ? f.getObservaciones() : "");
                row.createCell(8).setCellValue(f.getVendedorEmail() != null ? f.getVendedorEmail() : "—");
            }

            wb.write(out);

            // Nombre dinámico del archivo
            String fDesde = (fechaDesde != null && !fechaDesde.trim().isEmpty()) ? fechaDesde.trim() : "Inicio";
            String fHasta = (fechaHasta != null && !fechaHasta.trim().isEmpty()) ? fechaHasta.trim() : "Fin";
            String mPago = (metodoPago != null && !metodoPago.trim().isEmpty()) ? metodoPago.trim() : "Todos";
            String filename = String.format("Ventas_%s_%s_%s.xlsx", fDesde, fHasta, mPago);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
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
    public ResponseEntity<Map<String, Object>> guardarVenta(
            @RequestBody FacturaRequestDTO request,
            java.security.Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            String vendedorEmail = (principal != null) ? principal.getName() : "Consumidor Final";
            Factura factura = facturaService.crearFactura(request, vendedorEmail);
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