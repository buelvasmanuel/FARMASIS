package com.admin.adminlfarma_mini.repository;

import com.admin.adminlfarma_mini.entity.ResultadoOptimizacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResultadoOptimizacionRepository extends MongoRepository<ResultadoOptimizacion, String> {

    Optional<ResultadoOptimizacion> findTopByOrderByFechaCalculoDesc();
}
