package com.kadir.aipage.controller;

import com.kadir.aipage.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@Tag(
        name = "Sohbet İşlemleri",
        description = "Yapay zeka asistanı ile iletişim kurulan ana endpointler."
)
public class ChatController {

    private final AiChatService aiChatService;

    public ChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @Operation(
            summary = "Yapay zeka ile sohbet",
            description = "Kullanıcıdan gelen prompt'u yapay zeka servisine gönderir ve yapay zeka tarafından oluşturulan cevabı döndürür."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Başarılı - Yapay zeka yanıtı",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(
                                    example = "Amazon'da ürün 599 TL, Hepsiburada'da 649 TL..."
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Geçersiz istek - Boş prompt gönderildi",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Sunucu hatası - Yapay zeka servisine ulaşılamadı",
                    content = @Content
            )
    })
    @PostMapping(
            value = "/ask",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> askQuestion(
            @RequestBody String userMessage
    ) {

        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Prompt boş olamaz.");
        }

        String cevap = aiChatService.sendMessage(userMessage);

        return ResponseEntity.ok(cevap);
    }
}