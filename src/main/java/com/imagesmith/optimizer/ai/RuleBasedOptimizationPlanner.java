package com.imagesmith.optimizer.ai;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleBasedOptimizationPlanner implements OptimizationPlanner {
    @Override
    public OptimizationPlan generate(OptimizationContext context) {
        List<OptimizationPlan.OptimizationCandidate> candidates = new ArrayList<>();
        for (MapFinding finding : findings(context)) {
            if (finding.ruleCode().equals("DOCKER-012")) {
                candidates.add(candidate("MULTI_STAGE_BUILD", finding, "Move build-only dependencies into a builder stage."));
            } else if (finding.ruleCode().equals("SECURITY-006")) {
                candidates.add(candidate("NON_ROOT_RUNTIME", finding, "Create a dedicated non-root runtime user."));
            } else if (finding.ruleCode().equals("DOCKER-001")) {
                candidates.add(candidate("PIN_BASE_IMAGE", finding, "Replace the floating base image with a reviewed immutable reference."));
            }
        }
        return new OptimizationPlan(candidates);
    }

    private OptimizationPlan.OptimizationCandidate candidate(String type, MapFinding finding, String rationale) {
        return new OptimizationPlan.OptimizationCandidate(type, finding.filePath(), rationale,
                finding.evidence(), 0.82, finding.severity());
    }

    private List<MapFinding> findings(OptimizationContext context) {
        return context.securityFindings().stream()
                .map(finding -> new MapFinding(String.valueOf(finding.get("ruleCode")), String.valueOf(finding.get("severity")),
                        String.valueOf(finding.get("filePath")), List.of(String.valueOf(finding.get("title")))))
                .toList();
    }

    private record MapFinding(String ruleCode, String severity, String filePath, List<String> evidence) {}
}