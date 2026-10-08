package com.flowai.engine.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowNode {

    private String id;
    private String type;
    private Map<String, Object> config;

    /**
     * Formato de "next":
     * - String: "n2"
     * - List<String>: ["n2", "n3"]
     * - Map<String, List<String>>: { "then": ["n2"], "else": ["n3"] }  (IF)
     * - null: fim
     */
    private Object next;

    @SuppressWarnings("unchecked")
    public List<String> getNextList() {
        if (next == null) return List.of();
        if (next instanceof String s) return List.of(s);
        if (next instanceof List<?> l) {
            return l.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<String> getThenBranches() {
        if (next instanceof Map<?, ?> m) {
            Object then = ((Map<String, Object>) m).get("then");
            if (then instanceof String s) return List.of(s);
            if (then instanceof List<?> l) return l.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<String> getElseBranches() {
        if (next instanceof Map<?, ?> m) {
            Object els = ((Map<String, Object>) m).get("else");
            if (els instanceof String s) return List.of(s);
            if (els instanceof List<?> l) return l.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    public Map<String, Object> getConfigOrEmpty() {
        return config == null ? Map.of() : config;
    }
}