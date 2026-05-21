package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.DTO.ChatRequestDto;
import com.admin.adminlfarma_mini.DTO.ChatResponseDto;
import com.admin.adminlfarma_mini.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

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
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ChatResponseDto("Error interno: " + e.getMessage()));
        }
    }
}
