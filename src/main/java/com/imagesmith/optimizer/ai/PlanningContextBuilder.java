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
    private final SourceAnalyzer sourceAnalyzer;
    private final ObjectMapper objectMapper;

    public PlanningContextBuilder(InspectionSessionStore sessions, InspectionAnnotationStore annotations,
                                  KnowledgeRetriever knowledgeRetriever, SourceAnalyzer sourceAnalyzer, ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.annotations = annotations;
        this.knowledgeRetriever = knowledgeRetriever;
        this.sourceAnalyzer = sourceAnalyzer;
        this.objectMapper = objectMapper;
    }

    public OptimizationContext build(java.util.UUID sessionId) {
        InspectionSessionEntity session = sessions.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection session not found"));
        Map<String, Object> evidence = readMap(session.getEvidenceJson());
        List<InspectionAnnotationEntity> sessionAnnotations = annotations.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<SourceAnalyzer.StructuredFinding> sourceFindings = sourceAnalyzer.analyze(sessionId);
        Set<String> categories = sourceFindings.stream().map(SourceAnalyzer.StructuredFinding::category).collect(java.util.stream.Collectors.toSet());
        categories.addAll(sessionAnnotations.stream().map(InspectionAnnotationEntity::getCategory).toList());
        String runtime = inferRuntime(evidence);
        List<Map<String, Object>> findings = new java.util.ArrayList<>(sourceFindings.stream().map(this::findingMap).toList());
        findings.addAll(sessionAnnotations.stream().map(this::annotationMap).toList());
        return new OptimizationContext(
                firstDockerfile(evidence),
                mapValue(evidence, "categories"),
                mapValue(evidence, "summary"),
                mapValue(evidence, "dockerAssets"),
                findings,
                mapValue(evidence, "languages"),
                List.of(),
                knowledgeRetriever.retrieve(runtime, categories));
    }

    private String firstDockerfile(Map<String, Object> evidence) {
        Object dockerfiles = evidence.get("dockerfiles");
        if (dockerfiles instanceof List<?> list && !list.isEmpty()) {
            try {
                return objectMapper.writeValueAsString(list.getFirst());
            } catch (Exception exception) {
                throw new IllegalStateException("Dockerfile evidence could not be serialized", exception);
            }
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

    private Map<String, Object> findingMap(SourceAnalyzer.StructuredFinding finding) {
        return Map.of("category", finding.category(), "severity", finding.severity(), "ruleCode", finding.ruleCode(),
                "title", finding.title(), "detail", finding.detail(), "filePath", finding.filePath() == null ? "" : finding.filePath(),
                "evidence", finding.evidence());
    }

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Stored inspection evidence is invalid", exception);
        }
    }
}