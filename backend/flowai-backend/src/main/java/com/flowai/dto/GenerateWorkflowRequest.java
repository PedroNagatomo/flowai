package com.flowai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateWorkflowRequest(
        @NotBlank
        @Size(min = 5, max = 2000)
        String prompt
) {}