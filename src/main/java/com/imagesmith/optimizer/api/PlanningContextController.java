package com.imagesmith.optimizer.api;

import com.imagesmith.optimizer.ai.OptimizationContext;
import com.imagesmith.optimizer.ai.PlanningContextBuilder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/v1/inspection-sessions")
public class PlanningContextController {
    private final PlanningContextBuilder contextBuilder;

    public PlanningContextController(PlanningContextBuilder contextBuilder) {
        this.contextBuilder = contextBuilder;
    }

    @GetMapping("/{sessionId}/planning-context")
    public OptimizationContext context(@PathVariable UUID sessionId) {
        try {
            return contextBuilder.build(sessionId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }
}