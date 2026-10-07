package com.flowai.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class TemplateResolver {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");

    /**
     * Substitui todas as ocorrências de {{path}} pelo valor correspondente no contexto.
     * Se não encontrar, substitui por string vazia (logando warning).
     */
    public String resolve(String template, ExecutionContext ctx) {
        if (template == null || !template.contains("{{")) return template;

        Map<String, Object> root = ctx.toMap();
        Matcher m = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (m.find()) {
            String path = m.group(1);
            Object val = resolvePath(path, root);
            if (val == null) {
                log.warn("⚠️ Variável não resolvida: {}", path);
            }
            String replacement = val == null ? "" : stringify(val);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Object resolvePath(String path, Map<String, Object> root) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = ((Map<String, Object>) map).get(part);
            } else {
                return null;
            }
            if (current == null) return null;
        }
        return current;
    }

    private String stringify(Object val) {
        if (val instanceof Map || val instanceof java.util.List) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(val);
            } catch (Exception e) {
                return val.toString();
            }
        }
        return String.valueOf(val);
    }
}