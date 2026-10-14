package com.freightfox.githubsearch.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "github_repositories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubRepositoryEntity {
    @Id
    private Long id; // GitHub repository ID

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String owner;

    private String language;

    private Integer stars;

    private Integer forks;

    private Instant lastUpdated;
}
