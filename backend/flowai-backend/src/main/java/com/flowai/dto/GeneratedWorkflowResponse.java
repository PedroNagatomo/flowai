package com.flowai.dto;

import java.util.Map;

public record GeneratedWorkflowResponse(
        String name,
        String description,
        Map<String, Object> definition
) {}