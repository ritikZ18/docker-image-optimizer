package com.imagesmith.optimizer.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "repositories")
public class RepositoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String url;

    protected RepositoryEntity() {
    }

    public RepositoryEntity(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
}
