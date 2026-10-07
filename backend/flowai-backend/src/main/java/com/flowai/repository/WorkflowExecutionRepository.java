package com.flowai.repository;

import com.flowai.entity.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {
    List<WorkflowExecution> findTop50ByWorkflowIdOrderByStartedAtDesc(UUID workflowId);
}