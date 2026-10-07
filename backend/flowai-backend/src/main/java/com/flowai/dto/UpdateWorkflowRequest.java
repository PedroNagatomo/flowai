package com.flowai.dto;

import java.util.Map;

public record UpdateWorkflowRequest(
        String name,
        String description,
        Map<String, Object> definition,
        Boolean isActive
) {}