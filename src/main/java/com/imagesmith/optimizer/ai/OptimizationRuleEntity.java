package com.imagesmith.optimizer.ai;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "optimization_rules")
public class OptimizationRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true, nullable = false)
    private String ruleCode;
    private String category;
    private String language;
    private String applicability;
    private String sourceUrl;
    private String documentationVersion;
    private Instant lastVerifiedAt;
    private boolean active;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    protected OptimizationRuleEntity() {
    }

    public OptimizationRuleEntity(String ruleCode, String category, String language, String description,
                                  String applicability, String sourceUrl, String documentationVersion) {
        this.ruleCode = ruleCode;
        this.category = category;
        this.language = language;
        this.description = description;
        this.applicability = applicability;
        this.sourceUrl = sourceUrl;
        this.documentationVersion = documentationVersion;
        this.lastVerifiedAt = Instant.now();
        this.active = true;
    }

    public UUID getId() { return id; }
    public String getRuleCode() { return ruleCode; }
    public String getCategory() { return category; }
    public String getLanguage() { return language; }
    public String getDescription() { return description; }
    public String getApplicability() { return applicability; }
    public String getSourceUrl() { return sourceUrl; }
    public String getDocumentationVersion() { return documentationVersion; }
    public Instant getLastVerifiedAt() { return lastVerifiedAt; }
    public boolean isActive() { return active; }
}