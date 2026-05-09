package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.ConfiguracionSistema;
import com.admin.adminlfarma_mini.repository.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;

    public ConfiguracionSistema getConfiguracion() {
        return configuracionRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    ConfiguracionSistema nueva = new ConfiguracionSistema();
                    return configuracionRepository.save(nueva);
                });
    }

    @Transactional
    public ConfiguracionSistema guardarConfiguracion(ConfiguracionSistema configuracion) {
        ConfiguracionSistema actual = getConfiguracion();
        actual.setNombreFarmacia(configuracion.getNombreFarmacia());
        actual.setNit(configuracion.getNit());
        actual.setDireccion(configuracion.getDireccion());
        actual.setTelefono(configuracion.getTelefono());
        actual.setUmbralStockBajo(configuracion.getUmbralStockBajo());
        return configuracionRepository.save(actual);
    }
}
