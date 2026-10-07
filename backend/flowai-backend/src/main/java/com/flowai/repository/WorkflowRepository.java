package com.flowai.repository;

import com.flowai.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    List<Workflow> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Workflow> findByIdAndUserId(UUID id, UUID userId);
    List<Workflow> findByIsActiveTrue();
}