package com.imagesmith.optimizer.api;

import com.imagesmith.optimizer.repository.RepositoryEntity;
import com.imagesmith.optimizer.repository.RepositoryInspectionService;
import com.imagesmith.optimizer.repository.RepositoryStore;
import com.imagesmith.optimizer.repository.InspectionSessionEntity;
import com.imagesmith.optimizer.repository.InspectionSessionStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/v1/repositories")
public class RepositoryController {
    private final RepositoryStore repositories;
    private final RepositoryInspectionService inspectionService;
    private final InspectionSessionStore sessions;
    private final ObjectMapper objectMapper;

    public RepositoryController(RepositoryStore repositories, RepositoryInspectionService inspectionService,
                                InspectionSessionStore sessions, ObjectMapper objectMapper) {
        this.repositories = repositories;
        this.inspectionService = inspectionService;
        this.sessions = sessions;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RepositoryResponse create(@Valid @RequestBody CreateRepositoryRequest request) {
        RepositoryEntity repository = repositories.save(new RepositoryEntity(request.name(), request.url()));
        return new RepositoryResponse(repository.getId(), repository.getName(), repository.getUrl());
    }

    @PostMapping("/{repositoryId}/inspect")
    public InspectionResponse inspect(@PathVariable UUID repositoryId,
                                      @Valid @RequestBody InspectRequest request) {
        RepositoryEntity repository = repositories.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
        var inspection = inspectionService.inspect(repository, request.sessionName());
        String evidenceJson;
        try {
            evidenceJson = objectMapper.writeValueAsString(inspection);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Inspection could not be stored", exception);
        }
        var session = sessions.save(new InspectionSessionEntity(repository.getId(), request.sessionName(),
                inspection.summary().fileCount(), inspection.summary().lineCount(), inspection.dockerfiles().size(), evidenceJson));
        return new InspectionResponse(repository.getId(), session.getId(), inspection);
    }

    @GetMapping("/{repositoryId}/sessions")
    public java.util.List<SessionSummary> sessions(@PathVariable UUID repositoryId) {
        if (!repositories.existsById(repositoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found");
        }
        return sessions.findByRepositoryIdOrderByCreatedAtDesc(repositoryId).stream()
                .map(session -> new SessionSummary(session.getId(), session.getSessionName(), session.getCreatedAt(),
                        session.getFileCount(), session.getLineCount(), session.getDockerfileCount()))
                .toList();
    }

    @GetMapping("/sessions/{sessionId}")
    public StoredSessionResponse session(@PathVariable UUID sessionId) {
        InspectionSessionEntity session = sessions.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection session not found"));
        try {
            return new StoredSessionResponse(session.getId(), session.getRepositoryId(), session.getSessionName(),
                    session.getCreatedAt(), objectMapper.readTree(session.getEvidenceJson()));
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored inspection is invalid", exception);
        }
    }

    @ExceptionHandler(RepositoryInspectionService.RepositoryInspectionException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse inspectionFailure(RepositoryInspectionService.RepositoryInspectionException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    public record CreateRepositoryRequest(@NotBlank String name, @NotBlank String url) {}
    public record InspectRequest(@NotBlank String sessionName) {}
    public record RepositoryResponse(UUID repositoryId, String name, String url) {}
    public record InspectionResponse(UUID repositoryId, UUID sessionId, RepositoryInspectionService.Inspection inspection) {}
    public record SessionSummary(UUID sessionId, String sessionName, java.time.Instant createdAt,
                                 int fileCount, long lineCount, int dockerfileCount) {}
    public record StoredSessionResponse(UUID sessionId, UUID repositoryId, String sessionName,
                                        java.time.Instant createdAt, com.fasterxml.jackson.databind.JsonNode evidence) {}
    public record ErrorResponse(String error) {}
}
