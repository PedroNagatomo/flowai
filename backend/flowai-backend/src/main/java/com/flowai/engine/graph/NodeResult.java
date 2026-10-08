package com.flowai.engine.graph;

public record NodeResult(
        String nodeId,
        String type,
        boolean success,
        String message
) {}