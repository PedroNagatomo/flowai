package com.flowai.engine;

import com.flowai.entity.Workflow;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ExecutionContext {

    private final Workflow workflow;
    private final Map<String, Object> payload;
    private final LocalDateTime now;
    private final Map<String, Map<String, Object>> nodes = new HashMap<>();

    public ExecutionContext(Workflow workflow, Map<String, Object> payload) {
        this.workflow = workflow;
        this.payload = payload == null ? Map.of() : payload;
        this.now = LocalDateTime.now();
    }

    /**
     * Registra output de um nó pra interpolação em nós seguintes: {{nodes.{var}.output}}
     */
    public void setNodeOutput(String variableName, Object output) {
        nodes.computeIfAbsent(variableName, k -> new HashMap<>())
                .put("output", output);
    }

    /**
     * Snapshot do contexto pra interpolação. Sempre reflete estado atual dos nós.
     */
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
        map.put("nodes", new HashMap<>(nodes));
        return map;
    }

    public Workflow workflow() { return workflow; }
}