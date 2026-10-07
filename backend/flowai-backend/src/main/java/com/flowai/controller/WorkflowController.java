package com.flowai.controller;

import com.flowai.config.CurrentUser;
import com.flowai.dto.CreateWorkflowRequest;
import com.flowai.dto.UpdateWorkflowRequest;
import com.flowai.dto.WorkflowResponse;
import com.flowai.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.flowai.dto.GenerateWorkflowRequest;
import com.flowai.dto.GeneratedWorkflowResponse;
import com.flowai.service.AIGenerationService;
import com.flowai.entity.WorkflowExecution;
import com.flowai.repository.WorkflowExecutionRepository;
import com.flowai.service.WorkflowExecutorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService service;
    private final AIGenerationService aiService;
    private final WorkflowExecutorService executorService;
    private final WorkflowExecutionRepository executionRepo;


    @GetMapping
    public List<WorkflowResponse> list(@CurrentUser UUID userId) {
        return service.listByUser(userId);
    }

    @PostMapping
    public ResponseEntity<WorkflowResponse> create(
            @CurrentUser UUID userId,
            @Valid @RequestBody CreateWorkflowRequest req
    ) {
        return ResponseEntity.ok(service.create(userId, req));
    }

    @GetMapping("/{id}")
    public WorkflowResponse get(@CurrentUser UUID userId, @PathVariable UUID id) {
        return service.get(userId, id);
    }

    @PutMapping("/{id}")
    public WorkflowResponse update(
            @CurrentUser UUID userId,
            @PathVariable UUID id,
            @RequestBody UpdateWorkflowRequest req
    ) {
        return service.update(userId, id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generate")
    public GeneratedWorkflowResponse generate(@Valid @RequestBody GenerateWorkflowRequest req) {
        return aiService.generate(req.prompt());
    }

    @GetMapping("/{id}/executions")
    public List<WorkflowExecution> listExecutions(
            @CurrentUser UUID userId,
            @PathVariable UUID id
    ) {
        // Valida ownership
        service.get(userId, id);
        return executionRepo.findTop50ByWorkflowIdOrderByStartedAtDesc(id);
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<Map<String, Object>> runManually(
            @CurrentUser UUID userId,
            @PathVariable UUID id
    ) {
        var wf = service.get(userId, id);
        executorService.executeAsync(UUID.fromString(wf.id().toString()),
                Map.of("manual", true, "at", java.time.LocalDateTime.now().toString()));
        return ResponseEntity.accepted().body(Map.of("status", "accepted"));
    }


}