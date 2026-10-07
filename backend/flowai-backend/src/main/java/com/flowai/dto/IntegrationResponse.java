package com.flowai.dto;

import com.flowai.entity.Integration;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record IntegrationResponse(
        UUID id,
        String type,
        String name,
        Map<String, Object> config,
        Boolean isActive,
        LocalDateTime createdAt
) {
    public static IntegrationResponse from(Integration i) {
        return new IntegrationResponse(
                i.getId(),
                i.getType(),
                i.getName(),
                i.getConfig(),
                i.getIsActive(),
                i.getCreatedAt()
        );
    }
}