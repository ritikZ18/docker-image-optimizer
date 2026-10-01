package com.imagesmith.optimizer.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OptimizationRuleStore extends JpaRepository<OptimizationRuleEntity, UUID> {
    List<OptimizationRuleEntity> findByActiveTrueOrderByRuleCodeAsc();
}