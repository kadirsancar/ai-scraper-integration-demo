package com.kadir.aipage.controller;

import com.kadir.aipage.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@Tag(name = "Sohbet İşlemleri", description = "Yapay zeka asistanı ile iletişim kurulan ana endpointler.")
public class ChatController {



    private final AiChatService aiChatService;

    public ChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @Operation(
            summary = "SAF METİN SOHBETİ",
            description = "Kullanıcıdan gelem prompt alınır, yapay zeka modeline iletili ve üretilen mesaj düz metin olarak döndürülür."
    )

    @ApiResponses( value = {
            @ApiResponse(responseCode = "200", description ="Başarılı - Yapay zeka yanıtı" ,
            content = @Content(mediaType = "text/plain", schema = @Schema (example = "Kasko, aracınızı kazalara karşı koruyan..." ))),

            @ApiResponse(responseCode = "400", description = "Geçersiz istek - Boş metin gönderildi", content = @Content),

            @ApiResponse(responseCode = "500", description = "Sunucu hatası - Yapay zeka motoruna ulaşılamadı", content = @Content)
    })

    @PostMapping("/ask")
    public ResponseEntity<String> askQuestion(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Yapay zekaya sorulacak soru (prompt)",
                    required = true,
                    content = @Content(mediaType = "text/plain", schema = @Schema(example = "Bana kaskonun avantajlarını sayar mısın?"))
            )

            @RequestBody String userMessage) {



        String cevap = aiChatService.sendMessage(userMessage);

        return ResponseEntity.ok(cevap);
    }
}