package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.service.ClienteService;
import com.admin.adminlfarma_mini.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/empleado")
@PreAuthorize("hasRole('EMPLEADO')")
public class EmpleadoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ClienteService clienteService;

    @GetMapping("/pos")
    public String puntoDeVenta(Model model) {
        model.addAttribute("productos", productoService.getProductosDisponibles());
        model.addAttribute("clientes", clienteService.listarTodos());
        model.addAttribute("consumidorFinal", clienteService.getConsumidorFinal());
        return "empleado/pos";
    }
}
