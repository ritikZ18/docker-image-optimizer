package com.imagesmith.optimizer.run;

import com.imagesmith.optimizer.repository.RepositoryEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "optimization_runs")
public class OptimizationRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false)
    private RepositoryEntity repository;
    @Enumerated(EnumType.STRING)
    private RunStatus status;
    private Instant createdAt;

    protected OptimizationRunEntity() {
    }

    public OptimizationRunEntity(RepositoryEntity repository) {
        this.repository = repository;
        this.status = RunStatus.CREATED;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public RepositoryEntity getRepository() { return repository; }
    public RunStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
