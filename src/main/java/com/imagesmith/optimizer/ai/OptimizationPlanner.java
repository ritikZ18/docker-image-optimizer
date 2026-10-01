package com.imagesmith.optimizer.ai;

public interface OptimizationPlanner {
    OptimizationPlan generate(OptimizationContext context);
}