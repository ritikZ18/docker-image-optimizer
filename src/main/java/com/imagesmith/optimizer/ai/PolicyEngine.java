package com.imagesmith.optimizer.ai;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class PolicyEngine {
    private static final Set<String> ALLOWED_TRANSFORMATIONS = Set.of("MULTI_STAGE_BUILD", "NON_ROOT_RUNTIME", "PIN_BASE_IMAGE");

    public PolicyDecision validate(OptimizationPlan plan) {
        List<String> violations = plan.candidates().stream()
                .flatMap(candidate -> {
                    if (!ALLOWED_TRANSFORMATIONS.contains(candidate.type())) {
                        return java.util.stream.Stream.of("Unsupported transformation: " + candidate.type());
                    }
                    if (candidate.confidence() < 0.5) {
                        return java.util.stream.Stream.of("Confidence below threshold for: " + candidate.type());
                    }
                    return java.util.stream.Stream.<String>empty();
                })
                .toList();
        return new PolicyDecision(violations.isEmpty(), violations);
    }

    public record PolicyDecision(boolean approved, List<String> violations) {}
}