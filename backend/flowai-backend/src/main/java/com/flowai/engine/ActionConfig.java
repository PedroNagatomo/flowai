package com.flowai.engine;

import java.util.Map;

/**
 * Configuração de uma ação, extraída da definition do workflow.
 */
public record ActionConfig(
        ActionType type,
        Map<String, Object> config
) {}