package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.Producto;
import com.admin.adminlfarma_mini.repository.ProductoRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final ChatClient chatClient;
    private final ProductoRepository productoRepository;

    public ChatbotService(ChatClient.Builder chatClientBuilder,
                          ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
        this.chatClient = chatClientBuilder.build();
    }

    public String chat(String userMessage) {
        // Cargar inventario real de MongoDB
        String inventarioContext = buildInventarioContext();

        String systemPrompt = """
                Eres FarmaBot, el Asistente Inteligente de la farmacia FarmaSIS.
                Eres experto en farmacología y gestión de inventario farmacéutico.
                Responde siempre en español, de forma profesional, amable y concisa.
                
                INVENTARIO ACTUAL DE LA FARMACIA (datos en tiempo real de la base de datos):
                
                """ + inventarioContext + """
                
                Con base en este inventario real, puedes:
                - Consultar el stock y precio de cualquier medicamento.
                - Identificar productos con bajo stock (menos de 10 unidades).
                - Listar todos los productos disponibles.
                - Recomendar alternativas si un producto no está disponible.
                
                Cuando el usuario pregunte por productos, usa SIEMPRE los datos del inventario anterior.
                Si el inventario está vacío, infórmalo claramente.
                Sé directo, no inventes datos que no estén en el inventario proporcionado.
                """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }

    private String buildInventarioContext() {
        try {
            List<Producto> productos = productoRepository.findProductosDisponibles();

            if (productos.isEmpty()) {
                return "[No hay productos registrados en el sistema actualmente]";
            }

            return productos.stream()
                    .map(p -> String.format(
                            "- %s (Código: %s) | Precio: $%.2f | Stock: %d unidades | Presentación: %s | Categoría: %s",
                            p.getNombre() != null ? p.getNombre() : "N/A",
                            p.getCodigo() != null ? p.getCodigo() : "N/A",
                            p.getPrecio() != null ? p.getPrecio() : 0.0,
                            p.getCantidad() != null ? p.getCantidad() : 0,
                            p.getPresentacion() != null ? p.getPresentacion() : "N/A",
                            p.getCategoria() != null ? p.getCategoria() : "N/A"
                    ))
                    .collect(Collectors.joining("\n"));

        } catch (Exception e) {
            return "[Error al cargar el inventario: " + e.getMessage() + "]";
        }
    }
}
