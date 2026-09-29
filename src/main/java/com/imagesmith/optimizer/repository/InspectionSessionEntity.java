package com.imagesmith.optimizer.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inspection_sessions")
public class InspectionSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID repositoryId;
    private String sessionName;
    private Instant createdAt;
    private int fileCount;
    private long lineCount;
    private int dockerfileCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String evidenceJson;

    protected InspectionSessionEntity() {
    }

    public InspectionSessionEntity(UUID repositoryId, String sessionName, int fileCount, long lineCount,
                                    int dockerfileCount, String evidenceJson) {
        this.repositoryId = repositoryId;
        this.sessionName = sessionName;
        this.fileCount = fileCount;
        this.lineCount = lineCount;
        this.dockerfileCount = dockerfileCount;
        this.evidenceJson = evidenceJson;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRepositoryId() { return repositoryId; }
    public String getSessionName() { return sessionName; }
    public Instant getCreatedAt() { return createdAt; }
    public int getFileCount() { return fileCount; }
    public long getLineCount() { return lineCount; }
    public int getDockerfileCount() { return dockerfileCount; }
    public String getEvidenceJson() { return evidenceJson; }
}