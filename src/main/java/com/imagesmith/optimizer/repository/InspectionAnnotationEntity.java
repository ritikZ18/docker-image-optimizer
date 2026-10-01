package com.imagesmith.optimizer.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inspection_annotations")
public class InspectionAnnotationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID sessionId;
    private String category;
    private String severity;
    private String filePath;
    private Integer lineNumber;
    private String title;
    private Instant createdAt;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(columnDefinition = "TEXT")
    private String evidenceJson;

    protected InspectionAnnotationEntity() {
    }

    public InspectionAnnotationEntity(UUID sessionId, String category, String severity, String filePath,
                                      Integer lineNumber, String title, String detail, String evidenceJson) {
        this.sessionId = sessionId;
        this.category = category;
        this.severity = severity;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.title = title;
        this.detail = detail;
        this.evidenceJson = evidenceJson;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getSessionId() { return sessionId; }
    public String getCategory() { return category; }
    public String getSeverity() { return severity; }
    public String getFilePath() { return filePath; }
    public Integer getLineNumber() { return lineNumber; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public String getEvidenceJson() { return evidenceJson; }
    public Instant getCreatedAt() { return createdAt; }
}