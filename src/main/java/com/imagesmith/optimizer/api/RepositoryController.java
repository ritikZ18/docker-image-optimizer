package com.imagesmith.optimizer.api;

import com.imagesmith.optimizer.repository.RepositoryEntity;
import com.imagesmith.optimizer.repository.RepositoryStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {
    private final RepositoryStore repositories;

    public RepositoryController(RepositoryStore repositories) {
        this.repositories = repositories;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RepositoryResponse create(@Valid @RequestBody CreateRepositoryRequest request) {
        RepositoryEntity repository = repositories.save(new RepositoryEntity(request.name(), request.url()));
        return new RepositoryResponse(repository.getId(), repository.getName(), repository.getUrl());
    }

    public record CreateRepositoryRequest(@NotBlank String name, @NotBlank String url) {}
    public record RepositoryResponse(UUID repositoryId, String name, String url) {}
}
