package com.imagesmith.optimizer.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imagesmith.optimizer.repository.InspectionAnnotationEntity;
import com.imagesmith.optimizer.repository.InspectionAnnotationStore;
import com.imagesmith.optimizer.repository.InspectionSessionEntity;
import com.imagesmith.optimizer.repository.InspectionSessionStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PlanningContextBuilder {
    private final InspectionSessionStore sessions;
    private final InspectionAnnotationStore annotations;
    private final KnowledgeRetriever knowledgeRetriever;
    private final ObjectMapper objectMapper;

    public PlanningContextBuilder(InspectionSessionStore sessions, InspectionAnnotationStore annotations,
                                  KnowledgeRetriever knowledgeRetriever, ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.annotations = annotations;
        this.knowledgeRetriever = knowledgeRetriever;
        this.objectMapper = objectMapper;
    }

    public OptimizationContext build(java.util.UUID sessionId) {
        InspectionSessionEntity session = sessions.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection session not found"));
        Map<String, Object> evidence = readMap(session.getEvidenceJson());
        List<InspectionAnnotationEntity> sessionAnnotations = annotations.findBySessionIdOrderByCreatedAtAsc(sessionId);
        Set<String> categories = sessionAnnotations.stream().map(InspectionAnnotationEntity::getCategory).collect(java.util.stream.Collectors.toSet());
        String runtime = inferRuntime(evidence);
        return new OptimizationContext(
                firstDockerfile(evidence),
                mapValue(evidence, "categories"),
                mapValue(evidence, "summary"),
                mapValue(evidence, "dockerAssets"),
                sessionAnnotations.stream().map(this::annotationMap).toList(),
                mapValue(evidence, "languages"),
                List.of(),
                knowledgeRetriever.retrieve(runtime, categories));
    }

    private String firstDockerfile(Map<String, Object> evidence) {
        Object dockerfiles = evidence.get("dockerfiles");
        if (dockerfiles instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.getFirst());
        }
        return "";
    }

    private String inferRuntime(Map<String, Object> evidence) {
        String serialized = evidence.toString().toLowerCase();
        if (serialized.contains("python")) return "python";
        if (serialized.contains("java")) return "java";
        if (serialized.contains("node") || serialized.contains("typescript")) return "node";
        return "";
    }

    private Map<String, Object> mapValue(Map<String, Object> evidence, String key) {
        Object value = evidence.get(key);
        return value instanceof Map<?, ?> map ? objectMapper.convertValue(map, new TypeReference<>() {}) : Map.of("value", value == null ? "" : value);
    }

    private Map<String, Object> annotationMap(InspectionAnnotationEntity annotation) {
        return Map.of("category", annotation.getCategory(), "severity", annotation.getSeverity(),
                "title", annotation.getTitle(), "detail", annotation.getDetail(),
                "filePath", annotation.getFilePath() == null ? "" : annotation.getFilePath());
    }

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Stored inspection evidence is invalid", exception);
        }
    }
}