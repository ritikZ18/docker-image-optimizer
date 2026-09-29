package com.imagesmith.optimizer.run;

public enum RunStatus {
    CREATED,
    BASELINE_BUILD,
    ANALYSIS,
    AI_PLAN_GENERATION,
    CANDIDATE_BUILD,
    VALIDATION,
    COMPARISON,
    COMPLETED,
    FAILED
}
