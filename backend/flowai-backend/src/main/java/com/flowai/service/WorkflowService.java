package com.flowai.service;

import com.flowai.dto.workflow.CreateWorkflowRequest;
import com.flowai.dto.workflow.UpdateWorkflowRequest;
import com.flowai.dto.workflow.WorkflowResponse;
import com.flowai.entity.Workflow;
import com.flowai.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final WorkflowRepository workflowRepository;

    @Transactional(readOnly = true)
    public List<WorkflowResponse> listByUser(UUID userId) {
        return workflowRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(WorkflowResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowResponse get(UUID userId, UUID id) {
        Workflow w = workflowRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow não encontrado"));
        return WorkflowResponse.from(w);
    }

    @Transactional
    public WorkflowResponse create(UUID userId, CreateWorkflowRequest req) {
        validateDefinition(req.definition());

        Workflow w = Workflow.builder()
                .userId(userId)
                .name(req.name().trim())
                .description(req.description())
                .definition(req.definition())
                .isActive(false)
                .build();

        workflowRepository.save(w);
        log.info("📝 Workflow criado: {} (user {})", w.getId(), userId);
        return WorkflowResponse.from(w);
    }

    @Transactional
    public WorkflowResponse update(UUID userId, UUID id, UpdateWorkflowRequest req) {
        Workflow w = workflowRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow não encontrado"));

        if (req.name() != null) w.setName(req.name().trim());
        if (req.description() != null) w.setDescription(req.description());
        if (req.definition() != null) {
            validateDefinition(req.definition());
            w.setDefinition(req.definition());
        }
        if (req.isActive() != null) w.setIsActive(req.isActive());

        workflowRepository.save(w);
        log.info("✏️ Workflow atualizado: {}", id);
        return WorkflowResponse.from(w);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Workflow w = workflowRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow não encontrado"));
        workflowRepository.delete(w);
        log.info("🗑️ Workflow deletado: {}", id);
    }

    private void validateDefinition(java.util.Map<String, Object> def) {
        if (def == null) throw new IllegalArgumentException("Definition obrigatória");
        if (!def.containsKey("trigger")) throw new IllegalArgumentException("Falta 'trigger' na definition");
        if (!def.containsKey("actions")) throw new IllegalArgumentException("Falta 'actions' na definition");
        Object actions = def.get("actions");
        if (!(actions instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("'actions' deve ser uma lista não vazia");
        }
    }
}