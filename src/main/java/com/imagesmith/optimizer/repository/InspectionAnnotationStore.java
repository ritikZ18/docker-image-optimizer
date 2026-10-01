package com.imagesmith.optimizer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InspectionAnnotationStore extends JpaRepository<InspectionAnnotationEntity, UUID> {
    List<InspectionAnnotationEntity> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}