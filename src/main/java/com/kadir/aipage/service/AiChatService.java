package com.kadir.aipage.service;

import com.kadir.aipage.entity.ChatMessage;
import com.kadir.aipage.repository.ChatMessageRepository;
import com.kadir.aipage.mcp.ECommerceTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private final RestTemplate restTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final ECommerceTool eCommerceTool;

    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    public AiChatService(ChatMessageRepository chatMessageRepository,
                         ECommerceTool eCommerceTool) {
        this.restTemplate = new RestTemplate();
        this.chatMessageRepository = chatMessageRepository;
        this.eCommerceTool = eCommerceTool;
    }

    public record Message(String role, String content) {}

    public record Choice(Message message) {}

    public record OpenRouterResponse(List<Choice> choices) {}

    public String sendMessage(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            return "Lütfen bir ürün adı veya ürün URL'si girin.";
        }

        String trimmedPrompt = prompt.trim();

        /*
         * 1. Kullanıcı doğrudan ürün URL'si gönderdiyse
         */
        if (trimmedPrompt.startsWith("http://")
                || trimmedPrompt.startsWith("https://")) {

            System.out.println("🔗 Ürün URL'si algılandı.");

            String scraperResult =
                    eCommerceTool.getProductPrice(trimmedPrompt);

            String analysisPrompt =
                    """
                    Aşağıdaki bilgiler bir e-ticaret sitesinden canlı olarak
                    çekilmiştir.

                    %s

                    Kullanıcıya Türkçe ve kısa bir cevap ver.

                    Güncel fiyatı belirt.
                    Önceki fiyat varsa belirt.
                    İndirim oranı varsa belirt.

                    Verilen bilgiler dışında fiyat uydurma.
                    """.formatted(scraperResult);

            String response = callOpenRouter(analysisPrompt);

            saveMessage(prompt, response);

            return response;
        }

        /*
         * 2. Kullanıcı doğal bir cümle yazdıysa
         *
         * Örnek:
         *
         * "Casio GR-B300EC-1A'nın fiyatı ne kadar?"
         * "Casio GR-B300EC-1A nerede daha ucuz?"
         * "Bu saatin fiyatına bakar mısın Casio GR-B300EC-1A"
         */
        System.out.println("🤖 Ürün adı OpenRouter tarafından çıkarılıyor...");

        String productExtractionPrompt =
                """
                Aşağıdaki kullanıcı mesajından satın alınmak veya fiyatı
                araştırılmak istenen ürünün adını/modelini çıkar.

                Kullanıcı mesajı:
                %s

                Sadece ürün adını döndür.
                Açıklama yapma.
                Fiyat kelimesi, soru cümlesi veya başka kelimeler ekleme.

                Örnek:
                "Casio GR-B300EC-1A'nın fiyatı ne kadar?"
                -> Casio GR-B300EC-1A

                "iPhone 15 Pro nerede daha ucuz?"
                -> iPhone 15 Pro

                "Sony WH-1000XM5 fiyatına bakar mısın?"
                -> Sony WH-1000XM5
                """.formatted(trimmedPrompt);

        String productName =
                callOpenRouter(productExtractionPrompt).trim();

        if (productName.isBlank()) {

            String response =
                    callOpenRouter(trimmedPrompt);

            saveMessage(prompt, response);

            return response;
        }

        System.out.println(
                "🔍 Bulunan ürün: " + productName
        );

        /*
         * 3. Ürün adıyla bütün scraper'ları çalıştır
         */
        String scraperResults =
                eCommerceTool.analyzeProductByName(productName);

        /*
         * 4. Scraper sonuçlarını OpenRouter'a gönder
         */
        String comparisonPrompt =
                """
                Kullanıcı şu ürünün fiyatını araştırıyor:

                %s

                Aşağıdaki bilgiler e-ticaret sitelerine girilerek
                canlı olarak alınmıştır:

                %s

                Bu bilgileri analiz ederek kullanıcıya Türkçe cevap ver.

                Kurallar:
                - En ucuz güncel fiyatı açıkça belirt.
                - Hangi sitede olduğunu belirt.
                - Diğer sitelerdeki fiyatları kısaca karşılaştır.
                - Önceki fiyat varsa belirt.
                - İndirim oranı varsa belirt.
                - Bir sitede fiyat bulunamadıysa bunu söyleyebilirsin.
                - Kesinlikle yeni bir fiyat uydurma.
                - Sadece yukarıdaki scraper sonuçlarını kullan.
                """.formatted(productName, scraperResults);

        String finalResponse =
                callOpenRouter(comparisonPrompt);

        /*
         * 5. Sohbet geçmişine kaydet
         */
        saveMessage(prompt, finalResponse);

        return finalResponse;
    }

    private String callOpenRouter(String promptContent) {

        String url =
                "https://openrouter.ai/api/v1/chat/completions";

        Map<String, Object> requestBody =
                new HashMap<>();

        requestBody.put(
                "model",
                "openai/gpt-3.5-turbo"
        );

        requestBody.put(
                "messages",
                Collections.singletonList(
                        new Message("user", promptContent)
                )
        );

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.set(
                "Authorization",
                "Bearer " + openRouterApiKey
        );

        headers.set(
                "HTTP-Referer",
                "http://localhost:8081"
        );

        headers.set(
                "X-Title",
                "AI Price Tracker"
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        requestBody,
                        headers
                );

        try {

            OpenRouterResponse response =
                    restTemplate.postForObject(
                            url,
                            request,
                            OpenRouterResponse.class
                    );

            if (response != null
                    && response.choices() != null
                    && !response.choices().isEmpty()
                    && response.choices().get(0).message() != null) {

                return response
                        .choices()
                        .get(0)
                        .message()
                        .content();
            }

            return "Yapay zekadan yanıt alınamadı.";

        } catch (Exception e) {

            e.printStackTrace();

            return "OpenRouter API bağlantısı sırasında hata oluştu: "
                    + e.getMessage();
        }
    }

    private void saveMessage(
            String prompt,
            String response) {

        ChatMessage messageToSave =
                new ChatMessage();

        messageToSave.setUserPrompt(prompt);
        messageToSave.setAiResponse(response);
        messageToSave.setCreatedAt(
                LocalDateTime.now()
        );

        chatMessageRepository.save(
                messageToSave
        );
    }
}

