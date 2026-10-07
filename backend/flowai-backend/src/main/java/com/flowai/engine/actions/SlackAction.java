package com.flowai.engine.actions;

import com.flowai.engine.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackAction implements ActionExecutor {

    private final TemplateResolver resolver;
    private final RestClient.Builder restClientBuilder;

    @Override
    public ActionType supportedType() {
        return ActionType.SEND_SLACK;
    }

    @Override
    public ActionResult execute(ActionConfig cfg, ExecutionContext ctx) {
        var c = cfg.config();
        String webhookUrl = (String) c.get("webhookUrl");
        String message = resolver.resolve((String) c.get("message"), ctx);
        String channel = (String) c.get("channel"); // opcional, se webhook aceitar override

        // Fallback: usa variável de ambiente global
        if (webhookUrl == null || webhookUrl.isBlank()) {
            webhookUrl = System.getenv("SLACK_DEFAULT_WEBHOOK_URL");
        }

        if (webhookUrl == null || webhookUrl.isBlank()) {
            return ActionResult.failure(
                    "webhookUrl do Slack não configurado (defina no config da ação ou SLACK_DEFAULT_WEBHOOK_URL)");
        }

        try {
            var body = channel != null && !channel.isBlank()
                    ? Map.of("text", message, "channel", channel)
                    : Map.of("text", message);

            restClientBuilder.build()
                    .post()
                    .uri(webhookUrl)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("💬 Slack enviado: {}", message);
            return ActionResult.success("Slack enviado");
        } catch (Exception e) {
            log.error("❌ Falha ao enviar Slack", e);
            return ActionResult.failure("Falha no Slack: " + e.getMessage());
        }
    }
}