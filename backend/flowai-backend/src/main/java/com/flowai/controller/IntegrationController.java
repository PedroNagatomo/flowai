package com.flowai.controller;

import com.flowai.config.CurrentUser;
import com.flowai.dto.CreateIntegrationRequest;
import com.flowai.dto.IntegrationResponse;
import com.flowai.service.IntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService service;

    @GetMapping
    public List<IntegrationResponse> list(@CurrentUser UUID userId) {
        return service.list(userId);
    }

    @PostMapping
    public IntegrationResponse create(
            @CurrentUser UUID userId,
            @Valid @RequestBody CreateIntegrationRequest req
    ) {
        return service.create(userId, req);
    }

    @PutMapping("/{id}")
    public IntegrationResponse update(
            @CurrentUser UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateIntegrationRequest req
    ) {
        return service.update(userId, id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}