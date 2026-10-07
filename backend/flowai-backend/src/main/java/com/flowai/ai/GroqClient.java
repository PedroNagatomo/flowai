package com.flowai.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GroqClient {

    private final RestClient rest;
    private final String model;
    private final ObjectMapper mapper;

    public GroqClient(
            @Value("${app.groq.base-url}") String baseUrl,
            @Value("${app.groq.api-key}") String apiKey,
            @Value("${app.groq.model}") String model,
            RestClient.Builder restClientBuilder,
            ObjectMapper mapper
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ GROQ_API_KEY não configurada — geração por IA vai falhar");
        }
        this.model = model;
        this.mapper = mapper;
        this.rest = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Chama o chat completions da Groq e retorna o conteúdo da resposta como String.
     * Força o modelo a retornar JSON válido via response_format.
     */
    public String completeJson(String systemPrompt, String userPrompt) {
        var body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object")
        );

        try {
            var response = rest.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return extractContent(response);
        } catch (Exception e) {
            log.error("❌ Erro ao chamar Groq", e);
            throw new IllegalStateException("Falha ao consultar IA: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        var choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("IA não retornou resposta");
        }
        var message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}