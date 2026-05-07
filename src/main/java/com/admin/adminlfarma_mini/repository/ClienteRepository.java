package com.admin.adminlfarma_mini.repository;

import com.admin.adminlfarma_mini.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends MongoRepository<Cliente, String> {

    Optional<Cliente> findByCodigo(String codigo);

    Optional<Cliente> findByIdentificacion(String identificacion);

    Optional<Cliente> findByEsConsumidorFinalTrue();

    Page<Cliente> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(
            String nombre, String codigo, Pageable pageable);

    boolean existsByCodigo(String codigo);

    List<Cliente> findAllByOrderByNombreAsc();
}