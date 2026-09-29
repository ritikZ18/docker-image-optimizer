package com.imagesmith.optimizer.ai;

import java.util.List;
import java.util.Map;

public record OptimizationContext(
        String dockerfile,
        Map<String, Object> applicationProfile,
        Map<String, Object> baselineMetrics,
        Map<String, Object> layerAnalysis,
        List<Map<String, Object>> securityFindings,
        Map<String, Object> dependencyGraph,
        List<Map<String, Object>> policies,
        List<Map<String, Object>> knowledge
) {
}
