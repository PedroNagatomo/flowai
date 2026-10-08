package com.flowai.engine.actions;

import com.flowai.ai.GroqClient;
import com.flowai.engine.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AIPromptAction implements ActionExecutor {

    private final TemplateResolver resolver;
    private final GroqClient groq;

    @Override
    public ActionType supportedType() {
        return ActionType.AI_PROMPT;
    }

    @Override
    public ActionResult execute(ActionConfig cfg, ExecutionContext ctx) {
        var c = cfg.config();
        String promptTemplate = (String) c.get("prompt");
        String outputVariable = (String) c.getOrDefault("outputVariable", "output");
        String systemPrompt = (String) c.getOrDefault("systemPrompt",
                "Você é um assistente dentro de um workflow de automação. Responda de forma concisa e direta.");

        if (promptTemplate == null || promptTemplate.isBlank()) {
            return ActionResult.failure("Campo 'prompt' é obrigatório no AI_PROMPT");
        }

        String resolvedPrompt = resolver.resolve(promptTemplate, ctx);
        log.debug("🤖 AI_PROMPT ({} chars): {}", resolvedPrompt.length(), abreviate(resolvedPrompt, 100));

        try {
            String response = groq.completeText(systemPrompt, resolvedPrompt);

            // Registra output pra próximos nós usarem via {{nodes.X.output}}
            ctx.setNodeOutput(outputVariable, response);

            log.info("✅ AI_PROMPT retornou: {}", abreviate(response, 150));
            return ActionResult.success("IA respondeu: " + abreviate(response, 100));
        } catch (Exception e) {
            log.error("❌ Falha no AI_PROMPT", e);
            return ActionResult.failure("Falha na IA: " + e.getMessage());
        }
    }

    private String abreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}