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

    public AiChatService(ChatMessageRepository chatMessageRepository, ECommerceTool eCommerceTool) {
        this.restTemplate = new RestTemplate();
        this.chatMessageRepository = chatMessageRepository;
        this.eCommerceTool = eCommerceTool;
    }

    // OpenRouter / OpenAI yanıt formatına uygun record yapıları
    public record Message(String role, String content) {}
    public record Choice(Message message) {}
    public record OpenRouterResponse(List<Choice> choices) {}

    public String sendMessage(String prompt) {
        String finalAiResponse;
        String trimmedPrompt = prompt.trim();
        String lowerPrompt = trimmedPrompt.toLowerCase();

        // 1. Senaryo: Kullanıcı doğrudan bir ürün URL'si gönderdiyse
        if (trimmedPrompt.startsWith("http://") || trimmedPrompt.startsWith("https://")) {
            System.out.println("🤖 Doğrudan URL algılandı, tekil ürün fiyatı çekiliyor...");
            String scraperResult = eCommerceTool.getProductPrice(trimmedPrompt);

            String analysisPrompt = String.format(
                    "Aşağıda bir e-ticaret ürün URL'sinden taranan fiyat bilgisi bulunmaktadır. Bu bilgiyi kullanıcıya net ve anlaşılır bir Türkçe ile açıkla:\n\n%s",
                    scraperResult
            );
            finalAiResponse = callOpenRouter(analysisPrompt);

        }
        // 2. Senaryo: Kullanıcı fiyat, karşılaştırma veya ürün adı ile arama yapıyorsa
        else if (lowerPrompt.contains("fiyat") || lowerPrompt.contains("karşılaştır") || lowerPrompt.contains("en ucuz") || lowerPrompt.contains("nerede") || lowerPrompt.contains("iphone")) {
            System.out.println("🤖 Scraper araçları tetikleniyor, dinamik ürün adı ayıklanıyor...");

            // Cümledeki dolgu kelimelerini ve eklerini temizleyelim
            String productName = trimmedPrompt
                    .replaceAll("(?i)\\b(fiyatı|fiyatları|fiyat|karşılaştırması|karşılaştır|yap|en ucuz|nerede|kaç|için|ne|kadar|arama|bul)\\b", "")
                    .replace("karşılaştırması", "")
                    .trim();

            if (productName.isBlank()) {
                productName = "iphone 15";
            }

            System.out.println("🔍 Arama için temizlenen net ürün adı: " + productName);

            // Scraper'ların topladığı ham fiyat verileri
            String rawScraperData = eCommerceTool.analyzeProductByName(productName);

            // Toplanan ham verileri OpenRouter'a vererek akıllı bir karşılaştırma raporu hazırlatıyoruz
            String analysisPrompt = String.format(
                    "Aşağıda e-ticaret sitelerinden anlık olarak taranmış ürün fiyat bilgileri bulunmaktadır. " +
                            "Bu verileri analiz et ve kullanıcıya Türkçe olarak hangisinin en ucuz olduğunu, hangi sitede ne kadar olduğunu " +
                            "net ve düzenli bir karşılaştırma raporu halinde açıkla:\n\n%s",
                    rawScraperData
            );

            finalAiResponse = callOpenRouter(analysisPrompt);

        } else {
            // 3. Diğer genel sohbet istekleri doğrudan OpenRouter'a iletilir
            finalAiResponse = callOpenRouter(trimmedPrompt);
        }

        // Sonucu veritabanına kaydedip dönüyoruz
        saveMessage(prompt, finalAiResponse);
        return finalAiResponse;
    }

    private String callOpenRouter(String promptContent) {
        String url = "https://openrouter.ai/api/v1/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        // Test aşamasında düşük token tüketimi ve hızlı yanıt için gpt-3.5-turbo kullanıyoruz
        requestBody.put("model", "openai/gpt-3.5-turbo");
        requestBody.put("messages", Collections.singletonList(new Message("user", promptContent)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + openRouterApiKey);
        headers.set("HTTP-Referer", "http://localhost:8081");
        headers.set("X-Title", "AI Price Tracker");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            OpenRouterResponse response = restTemplate.postForObject(url, request, OpenRouterResponse.class);
            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                return response.choices().get(0).message().content();
            }
            return "Yapay zekadan yanıt alınamadı.";
        } catch (Exception e) {
            e.printStackTrace();
            return "OpenRouter API bağlantısı sırasında hata oluştu: " + e.getMessage();
        }
    }

    private void saveMessage(String prompt, String response) {
        ChatMessage messageToSave = new ChatMessage();
        messageToSave.setUserPrompt(prompt);
        messageToSave.setAiResponse(response);
        messageToSave.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(messageToSave);
    }
}