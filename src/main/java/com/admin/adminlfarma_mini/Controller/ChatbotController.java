package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.DTO.ChatRequestDto;
import com.admin.adminlfarma_mini.DTO.ChatResponseDto;
import com.admin.adminlfarma_mini.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatResponseDto> askQuestion(@RequestBody ChatRequestDto request) {
        try {
            String aiResponse = chatbotService.chat(request.message());
            return ResponseEntity.ok(new ChatResponseDto(aiResponse));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ChatResponseDto("Error interno: " + e.getMessage()));
        }
    }
}
