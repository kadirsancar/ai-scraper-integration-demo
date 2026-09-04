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

    public AiChatService(
            ChatMessageRepository chatMessageRepository,
            ECommerceTool eCommerceTool) {

        this.restTemplate = new RestTemplate();
        this.chatMessageRepository = chatMessageRepository;
        this.eCommerceTool = eCommerceTool;
    }

    public record Message(
            String role,
            String content
    ) {}

    public record Choice(
            Message message
    ) {}

    public record OpenRouterResponse(
            List<Choice> choices
    ) {}

    // =========================================================
    // ANA MESAJ METODU
    // =========================================================

    public String sendMessage(String prompt) {

        if (prompt == null || prompt.isBlank()) {

            return "Lütfen bir ürün adı veya ürün URL'si girin.";
        }

        String trimmedPrompt =
                prompt.trim();

        // =====================================================
        // 1. KULLANICI DİREKT ÜRÜN URL'Sİ GÖNDERDİ
        // =====================================================

        if (trimmedPrompt.startsWith("http://")
                || trimmedPrompt.startsWith("https://")) {

            System.out.println(
                    "🔗 Ürün URL'si algılandı."
            );

            String scraperResult =
                    eCommerceTool.getProductPrice(
                            trimmedPrompt
                    );

            /*
             * Scraper sonucundaki gerçek URL'yi al.
             */
            String productUrl =
                    extractProductUrl(
                            scraperResult
                    );

            /*
             * PRODUCT_URL bilgisini AI'ye göndermiyoruz.
             *
             * Çünkü URL'yi AI'nin değiştirmesini istemiyoruz.
             */
            String cleanScraperResult =
                    removeProductUrl(
                            scraperResult
                    );

            String analysisPrompt =
                    """
                    Aşağıdaki bilgiler bir e-ticaret sitesinden canlı olarak
                    çekilmiştir.

                    %s

                    Kullanıcıya Türkçe ve kısa bir cevap ver.

                    Güncel fiyatı belirt.
                    Önceki fiyat varsa belirt.
                    İndirim oranı varsa belirt.

                    Önceki fiyat bilgisi yoksa "Yok TL" veya "0 TL" yazma.
                    Bunun yerine sadece "-" kullan.

                    İndirim bilgisi yoksa "Yok" veya "%%0" yazma.
                    Bunun yerine sadece "-" kullan.

                    Verilen bilgiler dışında fiyat uydurma.

                    Markdown kullanabilirsin ancak ürün URL'si oluşturma.
                    """.formatted(
                            cleanScraperResult
                    );

            String response =
                    callOpenRouter(
                            analysisPrompt
                    );

            /*
             * URL'yi OpenRouter'dan bağımsız olarak ekliyoruz.
             */
            response =
                    addProductUrl(
                            response,
                            productUrl
                    );

            saveMessage(
                    prompt,
                    response
            );

            return response;
        }

        // =====================================================
        // 2. KULLANICI DOĞAL BİR CÜMLE YAZDI
        // =====================================================

        System.out.println(
                "🤖 Ürün adı OpenRouter tarafından çıkarılıyor..."
        );

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
                """.formatted(
                        trimmedPrompt
                );

        String productName =
                callOpenRouter(
                        productExtractionPrompt
                ).trim();

        if (productName.isBlank()) {

            String response =
                    callOpenRouter(
                            trimmedPrompt
                    );

            saveMessage(
                    prompt,
                    response
            );

            return response;
        }

        System.out.println(
                "🔍 Bulunan ürün: "
                        + productName
        );

        // =====================================================
        // 3. BÜTÜN SCRAPER'LARI ÇALIŞTIR
        // =====================================================

        String scraperResults =
                eCommerceTool.analyzeProductByName(
                        productName
                );

        /*
         * EN ÖNEMLİ KISIM:
         *
         * OpenRouter'a göndermeden önce gerçek URL'yi
         * scraper sonucundan ayırıyoruz.
         */
        String productUrl =
                extractProductUrl(
                        scraperResults
                );

        /*
         * PRODUCT_URL satırını AI'ye göndermiyoruz.
         */
        String cleanScraperResults =
                removeProductUrl(
                        scraperResults
                );

        // =====================================================
        // 4. SCRAPER SONUÇLARINI OPENROUTER'A GÖNDER
        // =====================================================

        String comparisonPrompt =
                """
                Kullanıcı şu ürünün fiyatını araştırıyor:

                %s

                Aşağıdaki bilgiler e-ticaret sitelerine girilerek
                canlı olarak alınmıştır:

                %s

                Bu bilgileri analiz ederek kullanıcıya Türkçe cevap ver.

                ÇOK ÖNEMLİ KURALLAR:

                1. Fiyat karşılaştırması yapılıyorsa MUTLAKA Markdown tablo oluştur.

                2. Tablo formatı:

                | Platform | Ürün | Güncel Fiyat | Önceki Fiyat | İndirim |
                |---|---|---:|---:|---:|
                | Amazon | ... | ... TL | ... | ... |
                | Hepsiburada | ... | ... TL | ... | ... |
                | Trendyol | ... | ... TL | ... | ... |
                | PttAVM | ... | ... TL | ... | ... |

                3. Scraper sonuçlarında bulunmayan bir platformu tabloya ekleme.

                4. Bir platformda fiyat bulunamadıysa:
                "Fiyat bulunamadı" yaz.

                5. Ürün isimlerini scraper sonuçlarından aynen kullan.

                6. Güncel fiyatları scraper sonuçlarından aynen kullan.
                Yeni veya tahmini fiyat oluşturma.

                7. ÖNCEKİ FİYAT YOKSA:
                "Yok", "Yok TL", "null", "null TL" veya benzeri ifadeler kullanma.
                Bunun yerine sadece "-" karakteri kullan.

                8. İNDİRİM BİLGİSİ YOKSA:
                "Yok" veya "Yok TL" yazma.
                Bunun yerine sadece "-" karakteri kullan.

                9. İndirim bilgisi varsa scraper sonucundaki bilgiyi aynen kullan.

                10. Güncel fiyatı en düşük olan ürünü belirle.

                11. En ucuz ürünü tablonun altında şu formatta belirt:

                **En Ucuz Fiyat:** Platform - Fiyat TL

                12. Fiyat karşılaştırması dışında gereksiz uzun açıklamalar yapma.

                13. Sadece yukarıdaki scraper sonuçlarını kullan.

                14. Scraper sonucunda bulunmayan hiçbir fiyatı veya bilgiyi tahmin etme.

                15. Markdown tablosunda sütun başlıklarının düzgün ayrıldığından emin ol.

                16. Örnek indirim formatı:
                %%10.00 indirim (99.99 TL Kazanç)

                17. Ürün URL'si oluşturma veya tahmin etme.
                URL bilgisi sistem tarafından ayrıca eklenecektir.
                """.formatted(
                        productName,
                        cleanScraperResults
                );

        String finalResponse =
                callOpenRouter(
                        comparisonPrompt
                );

        // =====================================================
        // 5. GERÇEK URL'Yİ AI CEVABINA EKLE
        // =====================================================

        finalResponse =
                addProductUrl(
                        finalResponse,
                        productUrl
                );

        // =====================================================
        // 6. SOHBET GEÇMİŞİNE KAYDET
        // =====================================================

        saveMessage(
                prompt,
                finalResponse
        );

        return finalResponse;
    }

    // =========================================================
    // PRODUCT URL ÇIKAR
    // =========================================================

    private String extractProductUrl(
            String scraperResult) {

        if (scraperResult == null
                || scraperResult.isBlank()) {

            return null;
        }

        String marker =
                "PRODUCT_URL:";

        int startIndex =
                scraperResult.indexOf(marker);

        if (startIndex == -1) {

            return null;
        }

        startIndex +=
                marker.length();

        int endIndex =
                scraperResult.indexOf(
                        "\n",
                        startIndex
                );

        if (endIndex == -1) {

            endIndex =
                    scraperResult.length();
        }

        String url =
                scraperResult
                        .substring(
                                startIndex,
                                endIndex
                        )
                        .trim();

        if (url.startsWith("http://")
                || url.startsWith("https://")) {

            return url;
        }

        return null;
    }

    // =========================================================
    // PRODUCT URL SATIRINI TEMİZLE
    // =========================================================

    private String removeProductUrl(
            String text) {

        if (text == null
                || text.isBlank()) {

            return text;
        }

        return text.replaceAll(
                "(?m)^PRODUCT_URL:.*\\R?",
                ""
        ).trim();
    }

    // =========================================================
    // URL'Yİ AI CEVABINA EKLE
    // =========================================================

    private String addProductUrl(
            String response,
            String productUrl) {

        if (response == null) {

            response = "";
        }

        if (productUrl == null
                || productUrl.isBlank()) {

            return response;
        }

        /*
         * URL'yi kullanıcıya açık şekilde ekliyoruz.
         *
         * Frontend bunu daha sonra gerçek butona
         * dönüştürebilir.
         */
        return response.trim()
                + "\n\n"
                + "🔗 Ürüne Git:\n"
                + productUrl;
    }

    // =========================================================
    // OPENROUTER
    // =========================================================

    private String callOpenRouter(
            String promptContent) {

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
                        new Message(
                                "user",
                                promptContent
                        )
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

    // =========================================================
    // CHAT KAYDI
    // =========================================================

    private void saveMessage(
            String prompt,
            String response) {

        ChatMessage messageToSave =
                new ChatMessage();

        messageToSave.setUserPrompt(
                prompt
        );

        messageToSave.setAiResponse(
                response
        );

        messageToSave.setCreatedAt(
                LocalDateTime.now()
        );

        chatMessageRepository.save(
                messageToSave
        );
    }
}