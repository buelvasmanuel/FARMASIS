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
}