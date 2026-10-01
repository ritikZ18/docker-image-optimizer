package com.imagesmith.optimizer.api;

import com.imagesmith.optimizer.ai.OptimizationContext;
import com.imagesmith.optimizer.ai.OptimizationPlan;
import com.imagesmith.optimizer.ai.OptimizationPlanner;
import com.imagesmith.optimizer.ai.PlanningContextBuilder;
import com.imagesmith.optimizer.ai.PolicyEngine;
import com.imagesmith.optimizer.ai.SourceAnalyzer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/v1/inspection-sessions")
public class PlanningController {
    private final SourceAnalyzer analyzer;
    private final PlanningContextBuilder contextBuilder;
    private final OptimizationPlanner planner;
    private final PolicyEngine policyEngine;

    public PlanningController(SourceAnalyzer analyzer, PlanningContextBuilder contextBuilder,
                              OptimizationPlanner planner, PolicyEngine policyEngine) {
        this.analyzer = analyzer;
        this.contextBuilder = contextBuilder;
        this.planner = planner;
        this.policyEngine = policyEngine;
    }

    @GetMapping("/{sessionId}/findings")
    public java.util.List<SourceAnalyzer.StructuredFinding> findings(@PathVariable UUID sessionId) {
        try {
            return analyzer.analyze(sessionId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @PostMapping("/{sessionId}/plan")
    public PlanResponse plan(@PathVariable UUID sessionId) {
        try {
            OptimizationContext context = contextBuilder.build(sessionId);
            OptimizationPlan plan = planner.generate(context);
            return new PlanResponse(plan, policyEngine.validate(plan));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    public record PlanResponse(OptimizationPlan plan, PolicyEngine.PolicyDecision policy) {}
}