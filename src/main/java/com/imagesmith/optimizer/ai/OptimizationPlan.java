package com.imagesmith.optimizer.ai;

import java.util.List;

public record OptimizationPlan(List<OptimizationCandidate> candidates) {
    public record OptimizationCandidate(
            String type,
            String target,
            String rationale,
            List<String> evidence,
            double confidence,
            String risk
    ) {
    }
}
