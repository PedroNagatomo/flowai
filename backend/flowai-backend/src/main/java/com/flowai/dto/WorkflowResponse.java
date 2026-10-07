package com.flowai.dto;

import com.flowai.entity.Workflow;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record WorkflowResponse(
        UUID id,
        String name,
        String description,
        Map<String, Object> definition,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastRunAt,
        LocalDateTime nextRunAt
) {
    public static WorkflowResponse from(Workflow w) {
        return new WorkflowResponse(
                w.getId(),
                w.getName(),
                w.getDescription(),
                w.getDefinition(),
                w.getIsActive(),
                w.getCreatedAt(),
                w.getUpdatedAt(),
                w.getLastRunAt(),
                w.getNextRunAt()
        );
    }
}