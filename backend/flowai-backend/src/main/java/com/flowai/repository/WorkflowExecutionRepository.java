package com.flowai.repository;

import com.flowai.entity.WorkflowExecution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    List<WorkflowExecution> findByWorkflowIdOrderByStartedAtDesc(UUID workflowId, Pageable pageable);

    long countByWorkflowId(UUID workflowId);
}