package com.imagesmith.optimizer.run;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OptimizationRunStore extends JpaRepository<OptimizationRunEntity, UUID> {
}
