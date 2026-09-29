package com.imagesmith.optimizer.api;

import com.imagesmith.optimizer.repository.RepositoryEntity;
import com.imagesmith.optimizer.repository.RepositoryInspectionService;
import com.imagesmith.optimizer.repository.RepositoryStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {
    private final RepositoryStore repositories;
    private final RepositoryInspectionService inspectionService;

    public RepositoryController(RepositoryStore repositories, RepositoryInspectionService inspectionService) {
        this.repositories = repositories;
        this.inspectionService = inspectionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RepositoryResponse create(@Valid @RequestBody CreateRepositoryRequest request) {
        RepositoryEntity repository = repositories.save(new RepositoryEntity(request.name(), request.url()));
        return new RepositoryResponse(repository.getId(), repository.getName(), repository.getUrl());
    }

    @PostMapping("/{repositoryId}/inspect")
    public InspectionResponse inspect(@PathVariable UUID repositoryId) {
        RepositoryEntity repository = repositories.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
        var inspection = inspectionService.inspect(repository);
        return new InspectionResponse(repository.getId(), inspection.foundDockerfile(), inspection.dockerfiles());
    }

    @ExceptionHandler(RepositoryInspectionService.RepositoryInspectionException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse inspectionFailure(RepositoryInspectionService.RepositoryInspectionException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    public record CreateRepositoryRequest(@NotBlank String name, @NotBlank String url) {}
    public record RepositoryResponse(UUID repositoryId, String name, String url) {}
    public record InspectionResponse(UUID repositoryId, boolean foundDockerfile,
                                     java.util.List<RepositoryInspectionService.DockerfileReport> dockerfiles) {}
    public record ErrorResponse(String error) {}
}
