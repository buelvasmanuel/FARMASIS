package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.DTO.FacturaRequestDTO;
import com.admin.adminlfarma_mini.entity.*;
import com.admin.adminlfarma_mini.repository.FacturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FacturaService {
    
    private final FacturaRepository facturaRepository;
    private final ProductoService productoService;
    private final ClienteService clienteService;
    
    public Page<Factura> listarFacturas(Pageable pageable) {
        return facturaRepository.findAllByOrderByFechaDesc(pageable);
    }
    
    public Double getVentasDelDia() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1);
        List<Factura> ventas = facturaRepository.findVentasDelDia(inicio, fin);
        return ventas.stream().mapToDouble(Factura::getTotal).sum();
    }
    
    public Long getCantidadVentasDelDia() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1);
        return facturaRepository.countVentasDelDia(inicio, fin);
    }
    
    public Factura crearFactura(FacturaRequestDTO request) {
        // Obtener cliente
        Cliente cliente;
        if (request.getClienteId() != null && !request.getClienteId().isEmpty()) {
            cliente = clienteService.obtenerPorId(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        } else {
            cliente = clienteService.getConsumidorFinal();
        }
        
        // Crear factura
        Factura factura = new Factura();
        factura.setNumeroFactura(generarNumeroFactura());
        factura.setClienteId(cliente.getId());
        factura.setClienteNombre(cliente.getNombre());
        factura.setFecha(LocalDateTime.now());
        factura.setMetodoPago(request.getMetodoPago() != null ? request.getMetodoPago() : "EFECTIVO");
        factura.setObservaciones(request.getObservaciones());
        
        double subtotal = 0.0;
        
        // Procesar detalles
        for (FacturaRequestDTO.DetalleDTO detalleDTO : request.getDetalles()) {
            Producto producto = productoService.obtenerPorId(detalleDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
            // Validar stock
            if (producto.getCantidad() < detalleDTO.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + producto.getNombre());
            }
            
            // Crear detalle
            DetalleFactura detalle = new DetalleFactura();
            detalle.setProductoId(producto.getId());
            detalle.setProductoNombre(producto.getNombre());
            detalle.setProductoCodigo(producto.getCodigo());
            detalle.setCantidad(detalleDTO.getCantidad());
            detalle.setPrecioUnitario(detalleDTO.getPrecioUnitario());
            detalle.setSubtotal(detalleDTO.getPrecioUnitario() * detalleDTO.getCantidad());
            
            factura.getDetalles().add(detalle);
            subtotal += detalle.getSubtotal();
            
            // Actualizar stock
            productoService.actualizarStock(producto.getId(), detalleDTO.getCantidad());
        }
        
        factura.setSubtotal(subtotal);
        factura.setIva(0.0);
        factura.setTotal(subtotal);
        
        return facturaRepository.save(factura);
    }
    
    private String generarNumeroFactura() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = facturaRepository.count() + 1;
        return String.format("FAC-%s-%05d", fecha, count);
    }
    
    public Optional<Factura> obtenerPorId(String id) {
        return facturaRepository.findById(id);
    }
}
