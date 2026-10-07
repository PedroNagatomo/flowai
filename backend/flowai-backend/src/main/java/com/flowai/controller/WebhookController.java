package com.flowai.controller;

import com.flowai.repository.WorkflowRepository;
import com.flowai.service.WorkflowExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WorkflowRepository workflowRepo;
    private final WorkflowExecutorService executor;

    @PostMapping("/{workflowId}")
    public ResponseEntity<Map<String, Object>> receive(
            @PathVariable UUID workflowId,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        var wf = workflowRepo.findById(workflowId)
                .filter(w -> Boolean.TRUE.equals(w.getIsActive()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Workflow não encontrado ou inativo"));

        log.info("📥 Webhook recebido: workflow={} payload={}", workflowId, payload);

        executor.executeAsync(workflowId, payload == null ? Map.of() : payload);

        return ResponseEntity.accepted().body(Map.of(
                "status", "accepted",
                "workflowId", workflowId.toString()
        ));
    }
}