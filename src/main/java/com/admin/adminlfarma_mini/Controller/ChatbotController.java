package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.DTO.ChatRequestDto;
import com.admin.adminlfarma_mini.DTO.ChatResponseDto;
import com.admin.adminlfarma_mini.service.ChatbotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    private static final Logger logger = Logger.getLogger(ChatbotController.class.getName());

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatResponseDto> askQuestion(@RequestBody ChatRequestDto request, Authentication authentication) {
        try {
            String userRole = "EMPLEADO"; // Fallback por defecto
            if (authentication != null && authentication.getAuthorities() != null) {
                for (var authority : authentication.getAuthorities()) {
                    String authStr = authority.getAuthority();
                    if (authStr.equals("ROLE_OWNER") || authStr.equals("OWNER")) {
                        userRole = "OWNER";
                        break;
                    } else if (authStr.equals("ROLE_ADMIN") || authStr.equals("ADMIN")) {
                        userRole = "ADMIN";
                    }
                }
            }
            
            String aiResponse = chatbotService.chat(request.chatId(), request.message(), userRole);
            return ResponseEntity.ok(new ChatResponseDto(aiResponse));
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.log(Level.WARNING, "Error de API externa (Groq): " + e.getStatusCode(), e);
            if (e.getStatusCode().value() == 429) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new ChatResponseDto("⏳ El servicio de IA está temporalmente ocupado. Intenta de nuevo en unos segundos."));
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ChatResponseDto("⚠️ Error al comunicarse con el servicio de IA. Intenta de nuevo."));
        } catch (org.springframework.web.client.RestClientException e) {
            logger.log(Level.WARNING, "Error de red/timeout al consultar IA", e);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(new ChatResponseDto("⏱️ La consulta tardó demasiado o hubo un problema de red. Intenta de nuevo."));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error inesperado en chatbot", e);
            return ResponseEntity.ok(new ChatResponseDto("⚠️ Error interno del asistente. Intenta de nuevo en unos momentos."));
        }
    }
}
