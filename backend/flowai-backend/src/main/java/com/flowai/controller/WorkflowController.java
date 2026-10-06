package com.flowai.controller;

import com.flowai.dto.workflow.CreateWorkflowRequest;
import com.flowai.dto.workflow.UpdateWorkflowRequest;
import com.flowai.dto.workflow.WorkflowResponse;
import com.flowai.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> list(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(workflowService.listByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> get(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.get(userId, id));
    }

    @PostMapping
    public ResponseEntity<WorkflowResponse> create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateWorkflowRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workflowService.create(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowResponse> update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @RequestBody UpdateWorkflowRequest req) {
        return ResponseEntity.ok(workflowService.update(userId, id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        workflowService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}