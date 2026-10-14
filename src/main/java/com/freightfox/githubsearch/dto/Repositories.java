package com.freightfox.githubsearch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Schema(description = "A GitHub repository record")
@Builder
@Data
public class Repositories {

    @Schema(description = "GitHub repository ID", example = "123456789")
    private Long id;

    @Schema(description = "Repository name", example = "spring-boot")
    private String name;

    @Schema(description = "Repository description", example = "Spring Boot helps you create production-grade applications")
    private String description;

    @Schema(description = "Repository owner login", example = "spring-projects")
    private String owner;

    @Schema(description = "Primary programming language", example = "Java")
    private String language;

    @Schema(description = "Number of stars", example = "75000")
    private Integer stars;

    @Schema(description = "Number of forks", example = "12000")
    private Integer forks;

    @Schema(description = "Last updated timestamp", example = "2026-10-14T10:15:30Z")
    private Instant lastUpdated;
}
