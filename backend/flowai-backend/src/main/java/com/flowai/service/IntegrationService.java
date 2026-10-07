package com.flowai.service;

import com.flowai.dto.CreateIntegrationRequest;
import com.flowai.dto.IntegrationResponse;
import com.flowai.entity.Integration;
import com.flowai.repository.IntegrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final IntegrationRepository repo;

    @Transactional(readOnly = true)
    public List<IntegrationResponse> list(UUID userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(IntegrationResponse::from)
                .toList();
    }

    @Transactional
    public IntegrationResponse create(UUID userId, CreateIntegrationRequest req) {
        Integration i = Integration.builder()
                .userId(userId)
                .type(req.type())
                .name(req.name())
                .config(req.config())
                .isActive(true)
                .build();
        return IntegrationResponse.from(repo.save(i));
    }

    @Transactional
    public IntegrationResponse update(UUID userId, UUID id, CreateIntegrationRequest req) {
        Integration i = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccessDeniedException("Integração não encontrada"));
        i.setType(req.type());
        i.setName(req.name());
        i.setConfig(req.config());
        return IntegrationResponse.from(repo.save(i));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Integration i = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AccessDeniedException("Integração não encontrada"));
        repo.delete(i);
    }

    /**
     * Busca integração ativa por tipo (usado pelas actions).
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Integration> findActiveByType(UUID userId, String type) {
        return repo.findFirstByUserIdAndTypeAndIsActiveTrue(userId, type);
    }
}