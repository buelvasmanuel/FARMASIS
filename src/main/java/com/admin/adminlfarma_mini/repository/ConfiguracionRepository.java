package com.admin.adminlfarma_mini.repository;

import com.admin.adminlfarma_mini.entity.ConfiguracionSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionRepository extends JpaRepository<ConfiguracionSistema, Long> {
}
