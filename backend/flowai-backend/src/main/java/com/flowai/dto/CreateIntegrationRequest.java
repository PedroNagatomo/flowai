package com.flowai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateIntegrationRequest(
        @NotBlank String type,
        @NotBlank String name,
        @NotNull Map<String, Object> config
) {}