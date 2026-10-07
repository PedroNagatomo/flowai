package com.flowai.repository;

import com.flowai.entity.Integration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationRepository extends JpaRepository<Integration, UUID> {
    List<Integration> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Integration> findByIdAndUserId(UUID id, UUID userId);
    Optional<Integration> findFirstByUserIdAndTypeAndIsActiveTrue(UUID userId, String type);
}