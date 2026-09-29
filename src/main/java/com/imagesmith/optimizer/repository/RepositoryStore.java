package com.imagesmith.optimizer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RepositoryStore extends JpaRepository<RepositoryEntity, UUID> {
}
