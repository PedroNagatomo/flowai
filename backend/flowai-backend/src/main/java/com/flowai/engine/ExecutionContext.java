package com.flowai.engine;

import com.flowai.entity.Workflow;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Contexto disponível durante a execução de um workflow.
 * Contém os dados que podem ser interpolados com {{...}}.
 */
public class ExecutionContext {

    private final Workflow workflow;
    private final Map<String, Object> payload;
    private final LocalDateTime now;

    public ExecutionContext(Workflow workflow, Map<String, Object> payload) {
        this.workflow = workflow;
        this.payload = payload == null ? Map.of() : payload;
        this.now = LocalDateTime.now();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("workflow", Map.of(
                "id", workflow.getId().toString(),
                "name", workflow.getName()
        ));
        map.put("trigger", Map.of("payload", payload));
        map.put("now", Map.of(
                "timestamp", now.toString(),
                "date", now.toLocalDate().toString(),
                "time", now.toLocalTime().toString()
        ));
        return map;
    }

    public Workflow workflow() { return workflow; }
}