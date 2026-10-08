package com.flowai.engine.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphDefinition {

    private List<WorkflowNode> nodes;

    public WorkflowNode findNode(String id) {
        if (nodes == null) return null;
        return nodes.stream()
                .filter(n -> id.equals(n.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retorna o nó inicial (o primeiro com type TRIGGER, ou o primeiro da lista).
     */
    public WorkflowNode findEntryNode() {
        if (nodes == null || nodes.isEmpty()) return null;
        return nodes.stream()
                .filter(n -> "TRIGGER".equals(n.getType()))
                .findFirst()
                .orElse(nodes.get(0));
    }
}