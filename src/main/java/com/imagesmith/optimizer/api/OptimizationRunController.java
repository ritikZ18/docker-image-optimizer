package com.imagesmith.optimizer.api;

import com.imagesmith.optimizer.repository.RepositoryStore;
import com.imagesmith.optimizer.run.OptimizationRunEntity;
import com.imagesmith.optimizer.run.OptimizationRunStore;
import com.imagesmith.optimizer.run.RunStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/optimization-runs")
public class OptimizationRunController {
    private final OptimizationRunStore runs;
    private final RepositoryStore repositories;

    public OptimizationRunController(OptimizationRunStore runs, RepositoryStore repositories) {
        this.runs = runs;
        this.repositories = repositories;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RunResponse create(@Valid @RequestBody CreateRunRequest request) {
        var repository = repositories.findById(request.repositoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
        OptimizationRunEntity run = runs.save(new OptimizationRunEntity(repository));
        return toResponse(run);
    }

    @GetMapping("/{runId}/status")
    public RunResponse status(@PathVariable UUID runId) {
        return runs.findById(runId).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Optimization run not found"));
    }

    private RunResponse toResponse(OptimizationRunEntity run) {
        return new RunResponse(run.getId(), run.getStatus(), run.getCreatedAt());
    }

    public record CreateRunRequest(@NotNull UUID repositoryId) {}
    public record RunResponse(UUID runId, RunStatus status, Instant createdAt) {}
}
