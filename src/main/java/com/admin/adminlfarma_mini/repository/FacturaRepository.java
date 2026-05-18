package com.admin.adminlfarma_mini.repository;

import com.admin.adminlfarma_mini.entity.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FacturaRepository extends MongoRepository<Factura, String> {

    Page<Factura> findAllByOrderByFechaDesc(Pageable pageable);

    List<Factura> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("{ 'fecha': { $gte: ?0, $lt: ?1 } }")
    List<Factura> findVentasDelDia(LocalDateTime inicio, LocalDateTime fin);

    @Query(value = "{ 'fecha': { $gte: ?0, $lt: ?1 } }", count = true)
    long countVentasDelDia(LocalDateTime inicio, LocalDateTime fin);

    // Filtros avanzados para historial de ventas
    @Query("{ 'fecha': { $gte: ?0, $lte: ?1 } }")
    Page<Factura> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    @Query("{ 'fecha': { $gte: ?0, $lte: ?1 }, 'metodoPago': ?2 }")
    Page<Factura> findByFechaBetweenAndMetodoPago(LocalDateTime inicio, LocalDateTime fin, String metodoPago, Pageable pageable);

    @Query("{ 'numeroFactura': { $regex: ?0, $options: 'i' } }")
    Page<Factura> findByNumeroFactura(String numeroFactura, Pageable pageable);

    @Query("{ 'metodoPago': ?0 }")
    Page<Factura> findByMetodoPago(String metodoPago, Pageable pageable);

    // Para exportación (sin paginación)
    @Query("{ 'fecha': { $gte: ?0, $lte: ?1 } }")
    List<Factura> findByFechaRango(LocalDateTime inicio, LocalDateTime fin);

    @Query("{ 'fecha': { $gte: ?0, $lte: ?1 }, 'metodoPago': ?2 }")
    List<Factura> findByFechaRangoAndMetodoPago(LocalDateTime inicio, LocalDateTime fin, String metodoPago);
}