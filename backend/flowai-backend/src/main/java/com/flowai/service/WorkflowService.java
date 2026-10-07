package com.flowai.service;

import com.flowai.dto.CreateWorkflowRequest;
import com.flowai.dto.UpdateWorkflowRequest;
import com.flowai.dto.WorkflowResponse;
import com.flowai.entity.Workflow;
import com.flowai.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository repo;

    @Transactional(readOnly = true)
    public List<WorkflowResponse> listByUser(UUID userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WorkflowResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowResponse get(UUID userId, UUID id) {
        return WorkflowResponse.from(findOwned(userId, id));
    }

    @Transactional
    public WorkflowResponse create(UUID userId, CreateWorkflowRequest req) {
        Workflow w = Workflow.builder()
                .userId(userId)
                .name(req.name())
                .description(req.description())
                .definition(req.definition())
                .isActive(false)
                .build();
        return WorkflowResponse.from(repo.save(w));
    }

    @Transactional
    public WorkflowResponse update(UUID userId, UUID id, UpdateWorkflowRequest req) {
        Workflow w = findOwned(userId, id);
        if (req.name() != null && !req.name().isBlank()) w.setName(req.name());
        if (req.description() != null) w.setDescription(req.description());
        if (req.definition() != null) w.setDefinition(req.definition());
        if (req.isActive() != null) w.setIsActive(req.isActive());
        return WorkflowResponse.from(repo.save(w));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Workflow w = findOwned(userId, id);
        repo.delete(w);
    }

    private Workflow findOwned(UUID userId, UUID id) {
        return repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccessDeniedException("Workflow não encontrado ou sem permissão"));
    }
}