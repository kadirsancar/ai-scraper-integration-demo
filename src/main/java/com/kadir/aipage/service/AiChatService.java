package com.kadir.aipage.service;

import com.kadir.aipage.entity.ChatMessage;
import com.kadir.aipage.repository.ChatMessageRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
public class AiChatService {


    private final RestTemplate restTemplate;

    private final ChatMessageRepository chatMessageRepository;


    public AiChatService(ChatMessageRepository chatMessageRepository) {

        this.restTemplate = new RestTemplate();
        this.chatMessageRepository=chatMessageRepository;
    }


    public record AiResponseDto(String yanit) {
    }

    public String sendMessage(String prompt) {

        String url = "http://10.10.20.42:7000/chat";


        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("prompt", prompt);
        formData.add("system", "");
        formData.add("model", "");

        // Header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        // Request
        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(formData, headers);

        try {

            AiResponseDto responseDto = restTemplate.postForObject(
                    url,
                    request,
                    AiResponseDto.class
            );


            if (responseDto != null && responseDto.yanit() != null) {
                System.out.println("AI Response (Temizlenmiş): " + responseDto.yanit());


                ChatMessage messageToSave = new ChatMessage();

                messageToSave.setUserPrompt(prompt);
                messageToSave.setAiResponse(responseDto.yanit);
                messageToSave.setCreatedAt(LocalDateTime.now());

                chatMessageRepository.save(messageToSave);

                return responseDto.yanit();
            } else {
                return "Yapay zekadan geçerli bir yanıt alınamadı.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "AI servisine istek gönderilirken hata oluştu.",
                    e
            );
        }


    }
}
