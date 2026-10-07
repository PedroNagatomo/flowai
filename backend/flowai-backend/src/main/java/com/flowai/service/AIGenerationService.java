package com.flowai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowai.ai.GroqClient;
import com.flowai.ai.PromptBuilder;
import com.flowai.dto.GeneratedWorkflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIGenerationService {

    private static final List<String> VALID_TRIGGERS =
            List.of("WEBHOOK", "SCHEDULE", "EMAIL_RECEIVED", "MANUAL");

    private static final List<String> VALID_ACTIONS =
            List.of("SEND_EMAIL", "SEND_SLACK", "HTTP_REQUEST");

    private final GroqClient groq;
    private final PromptBuilder prompts;
    private final ObjectMapper mapper;

    public GeneratedWorkflowResponse generate(String userPrompt) {
        String raw = groq.completeJson(prompts.systemPrompt(), prompts.userPrompt(userPrompt));
        log.debug("🤖 IA retornou: {}", raw);

        Map<String, Object> json = parseAndValidate(raw);

        String name = (String) json.get("name");
        String description = (String) json.get("description");

        @SuppressWarnings("unchecked")
        Map<String, Object> definition = (Map<String, Object>) json.get("definition");

        return new GeneratedWorkflowResponse(name, description, definition);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAndValidate(String raw) {
        Map<String, Object> json;
        try {
            json = mapper.readValue(raw, Map.class);
        } catch (JsonProcessingException e) {
            log.error("❌ IA retornou JSON inválido: {}", raw);
            throw new IllegalStateException("IA retornou formato inválido. Tente reformular a descrição.");
        }

        if (!json.containsKey("name") || !json.containsKey("definition")) {
            throw new IllegalStateException("IA não retornou campos obrigatórios (name, definition)");
        }

        var definition = (Map<String, Object>) json.get("definition");
        if (definition == null) {
            throw new IllegalStateException("IA não retornou 'definition'");
        }

        // Valida trigger
        var trigger = (Map<String, Object>) definition.get("trigger");
        if (trigger == null) {
            throw new IllegalStateException("Workflow sem 'trigger'");
        }
        String triggerType = (String) trigger.get("type");
        if (!VALID_TRIGGERS.contains(triggerType)) {
            throw new IllegalStateException("Trigger inválido: " + triggerType);
        }

        // Valida actions
        var actions = (List<Map<String, Object>>) definition.get("actions");
        if (actions == null || actions.isEmpty()) {
            throw new IllegalStateException("Workflow precisa ter pelo menos 1 ação");
        }
        for (var action : actions) {
            String actionType = (String) action.get("type");
            if (!VALID_ACTIONS.contains(actionType)) {
                throw new IllegalStateException("Ação inválida: " + actionType);
            }
        }

        // Garante que "conditions" e "config" existem
        definition.putIfAbsent("conditions", List.of());
        trigger.putIfAbsent("config", Map.of());

        return json;
    }
}