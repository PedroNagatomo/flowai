package com.flowai.repository;

import com.flowai.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

    List<Workflow> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Workflow> findByIdAndUserId(UUID id, UUID userId);

    List<Workflow> findByIsActiveTrue();
}