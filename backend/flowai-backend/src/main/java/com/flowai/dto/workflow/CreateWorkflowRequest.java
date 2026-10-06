package com.flowai.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateWorkflowRequest(
        @NotBlank String name,
        String description,
        @NotNull Map<String, Object> definition
) {}