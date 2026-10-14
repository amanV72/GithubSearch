package com.freightfox.githubsearch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Request body for searching GitHub repositories")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GithubSearchRequest {

    @Schema(description = "Search keyword or phrase", example = "spring boot")
    @NotBlank(message = "Search query cannot be empty")
    private String query;

    @Schema(description = "Optional programming language filter", example = "Java", nullable = true)
    private String language;

    @Schema(description = "Sort results by: stars, forks, or updated. Defaults to stars if omitted.",
            example = "stars", allowableValues = {"stars", "forks", "updated"}, nullable = true)
    @Pattern(
            regexp = "^(stars|forks|updated)$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Sort must be one of: stars, forks, or updated"
    )
    private String sort;
}
