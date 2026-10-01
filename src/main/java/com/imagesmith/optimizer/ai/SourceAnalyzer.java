package com.imagesmith.optimizer.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imagesmith.optimizer.repository.InspectionSessionEntity;
import com.imagesmith.optimizer.repository.InspectionSessionStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SourceAnalyzer {
    private final InspectionSessionStore sessions;
    private final ObjectMapper objectMapper;

    public SourceAnalyzer(InspectionSessionStore sessions, ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.objectMapper = objectMapper;
    }

    public List<StructuredFinding> analyze(UUID sessionId) {
        InspectionSessionEntity session = sessions.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection session not found"));
        try {
            JsonNode evidence = objectMapper.readTree(session.getEvidenceJson());
            JsonNode dockerfiles = evidence.path("dockerfiles");
            List<StructuredFinding> findings = new ArrayList<>();
            if (!dockerfiles.isArray() || dockerfiles.isEmpty()) {
                findings.add(new StructuredFinding("CONTAINERS", "HIGH", "NO_DOCKERFILE", null, null,
                        "No Dockerfile found", "A candidate image cannot be built until the repository provides a Dockerfile.", List.of("dockerfiles=[]")));
                return findings;
            }
            for (JsonNode dockerfile : dockerfiles) {
                analyzeDockerfile(dockerfile, findings);
            }
            return findings;
        } catch (Exception exception) {
            throw new IllegalStateException("Stored inspection evidence is invalid", exception);
        }
    }

    private void analyzeDockerfile(JsonNode dockerfile, List<StructuredFinding> findings) {
        String path = dockerfile.path("path").asText();
        JsonNode instructions = dockerfile.path("instructions");
        List<String> normalized = new ArrayList<>();
        instructions.forEach(node -> normalized.add(node.asText().toUpperCase(Locale.ROOT)));
        long fromCount = normalized.stream().filter("FROM"::equals).count();
        if (fromCount < 2) {
            findings.add(new StructuredFinding("CONTAINERS", "MEDIUM", "DOCKER-012", path, instructionLine(instructions, "FROM"),
                    "Build and runtime stages are not separated", "Build dependencies may remain in the final image; evaluate a multi-stage build.", List.of(path + ":FROM", "FROM count=" + fromCount)));
        }
        if (!normalized.contains("USER")) {
            findings.add(new StructuredFinding("SECURITY", "HIGH", "SECURITY-006", path, null,
                    "Container does not declare a non-root user", "The final image may run as root; add and validate a dedicated runtime user.", List.of(path + ":USER missing")));
        }
        dockerfile.path("baseImages").forEach(image -> {
            String value = image.asText();
            if (value.endsWith(":latest") || !value.contains(":")) {
                findings.add(new StructuredFinding("SECURITY", "MEDIUM", "DOCKER-001", path, instructionLine(instructions, "FROM"),
                        "Base image is not pinned", "Use an explicit version or immutable digest instead of a floating base image.", List.of("base=" + value)));
            }
        });
    }

    private Integer instructionLine(JsonNode instructions, String instruction) {
        for (int index = 0; index < instructions.size(); index++) {
            if (instruction.equalsIgnoreCase(instructions.get(index).asText())) return index + 1;
        }
        return null;
    }

    public record StructuredFinding(String category, String severity, String ruleCode, String filePath,
                                    Integer lineNumber, String title, String detail, List<String> evidence) {}
}