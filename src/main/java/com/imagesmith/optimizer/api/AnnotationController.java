package com.imagesmith.optimizer.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imagesmith.optimizer.repository.InspectionAnnotationEntity;
import com.imagesmith.optimizer.repository.InspectionAnnotationStore;
import com.imagesmith.optimizer.repository.InspectionSessionStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/v1/inspection-sessions/{sessionId}/annotations")
public class AnnotationController {
    private final InspectionSessionStore sessions;
    private final InspectionAnnotationStore annotations;
    private final ObjectMapper objectMapper;

    public AnnotationController(InspectionSessionStore sessions, InspectionAnnotationStore annotations,
                                ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.annotations = annotations;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<AnnotationResponse> list(@PathVariable UUID sessionId) {
        requireSession(sessionId);
        return annotations.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnotationResponse create(@PathVariable UUID sessionId,
                                     @Valid @RequestBody CreateAnnotationRequest request) {
        requireSession(sessionId);
        JsonNode evidence = null;
        if (request.evidenceJson() != null && !request.evidenceJson().isBlank()) {
            try {
                evidence = objectMapper.readTree(request.evidenceJson());
            } catch (Exception exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "evidenceJson must be valid JSON", exception);
            }
        }
        var annotation = annotations.save(new InspectionAnnotationEntity(sessionId, request.category(), request.severity(),
                request.filePath(), request.lineNumber(), request.title(), request.detail(), evidence == null ? null : evidence.toString()));
        return toResponse(annotation);
    }

    private void requireSession(UUID sessionId) {
        if (!sessions.existsById(sessionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection session not found");
        }
    }

    private AnnotationResponse toResponse(InspectionAnnotationEntity annotation) {
        JsonNode evidence = null;
        if (annotation.getEvidenceJson() != null) {
            try {
                evidence = objectMapper.readTree(annotation.getEvidenceJson());
            } catch (Exception exception) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored annotation evidence is invalid", exception);
            }
        }
        return new AnnotationResponse(annotation.getId(), annotation.getSessionId(), annotation.getCategory(),
                annotation.getSeverity(), annotation.getFilePath(), annotation.getLineNumber(), annotation.getTitle(),
                annotation.getDetail(), evidence, annotation.getCreatedAt());
    }

    public record CreateAnnotationRequest(
            @NotBlank String category,
            @NotBlank String severity,
            String filePath,
            @Positive Integer lineNumber,
            @NotBlank String title,
            @NotBlank String detail,
            String evidenceJson) {}

    public record AnnotationResponse(UUID annotationId, UUID sessionId, String category, String severity,
                                     String filePath, Integer lineNumber, String title, String detail,
                                     JsonNode evidence, java.time.Instant createdAt) {}
}