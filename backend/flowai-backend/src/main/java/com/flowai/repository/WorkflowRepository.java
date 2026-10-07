package com.flowai.repository;

import com.flowai.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    List<Workflow> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Workflow> findByIdAndUserId(UUID id, UUID userId);
    List<Workflow> findByIsActiveTrue();
    @Query("""
    SELECT w FROM Workflow w
    WHERE w.isActive = true
      AND w.nextRunAt IS NOT NULL
      AND w.nextRunAt <= :now
""")
    List<Workflow> findDueWorkflows(@Param("now") LocalDateTime now);
}