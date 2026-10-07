package com.flowai.engine.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowai.engine.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpRequestAction implements ActionExecutor {

    private final TemplateResolver resolver;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper mapper;

    @Override
    public ActionType supportedType() {
        return ActionType.HTTP_REQUEST;
    }

    @Override
    public ActionResult execute(ActionConfig cfg, ExecutionContext ctx) {
        var c = cfg.config();
        String method = (String) c.getOrDefault("method", "POST");
        String url = resolver.resolve((String) c.get("url"), ctx);
        String bodyRaw = resolver.resolve((String) c.get("body"), ctx);

        @SuppressWarnings("unchecked")
        Map<String, String> headers = c.get("headers") instanceof Map
                ? (Map<String, String>) c.get("headers")
                : Map.of();

        if (url == null || url.isBlank()) {
            return ActionResult.failure("Campo 'url' é obrigatório");
        }

        try {
            var spec = restClientBuilder.build()
                    .method(HttpMethod.valueOf(method.toUpperCase()))
                    .uri(url);

            headers.forEach(spec::header);

            if (bodyRaw != null && !bodyRaw.isBlank()) {
                spec.body(bodyRaw);
            }

            var response = spec.retrieve().toEntity(String.class);
            log.info("🌐 {} {} → {}", method, url, response.getStatusCode());

            return ActionResult.success(method + " " + url + " → " + response.getStatusCode());
        } catch (Exception e) {
            log.error("❌ Falha na requisição HTTP", e);
            return ActionResult.failure("Falha HTTP: " + e.getMessage());
        }
    }
}