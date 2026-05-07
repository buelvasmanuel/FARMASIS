package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.Cliente;
import com.admin.adminlfarma_mini.repository.ClienteRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @PostConstruct
    public void init() {
        crearConsumidorFinalSiNoExiste();
    }

    private void crearConsumidorFinalSiNoExiste() {
        if (clienteRepository.findByEsConsumidorFinalTrue().isEmpty()) {
            Cliente consumidorFinal = new Cliente();
            consumidorFinal.setCodigo("CF-0001");
            consumidorFinal.setNombre("Consumidor Final");
            consumidorFinal.setIdentificacion("9999999999");
            consumidorFinal.setEsConsumidorFinal(true);
            clienteRepository.save(consumidorFinal);
            System.out.println("✅ Cliente 'Consumidor Final' creado en MongoDB");
        }
    }

    public Cliente getConsumidorFinal() {
        return clienteRepository.findByEsConsumidorFinalTrue()
                .orElseThrow(() -> new RuntimeException("No se encontró el cliente Consumidor Final"));
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAllByOrderByNombreAsc();
    }

    public Page<Cliente> listarClientes(Pageable pageable) {
        return clienteRepository.findAll(pageable);
    }

    public Page<Cliente> buscarClientes(String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return listarClientes(pageable);
        }
        return clienteRepository.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(
                search, search, pageable);
    }

    public Optional<Cliente> obtenerPorId(String id) {
        return clienteRepository.findById(id);
    }

    public Optional<Cliente> obtenerPorCodigo(String codigo) {
        return clienteRepository.findByCodigo(codigo);
    }

    public Cliente guardar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public void eliminar(String id) {
        clienteRepository.deleteById(id);
    }
}