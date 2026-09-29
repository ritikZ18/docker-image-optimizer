package com.imagesmith.optimizer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InspectionSessionStore extends JpaRepository<InspectionSessionEntity, UUID> {
    List<InspectionSessionEntity> findByRepositoryIdOrderByCreatedAtDesc(UUID repositoryId);
}