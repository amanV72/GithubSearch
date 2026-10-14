package com.freightfox.githubsearch.controller;

import com.freightfox.githubsearch.dto.ErrorResponse;
import com.freightfox.githubsearch.dto.GithubSearchRequest;
import com.freightfox.githubsearch.dto.GithubSearchResponse;
import com.freightfox.githubsearch.dto.Repositories;
import com.freightfox.githubsearch.services.GithubSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Tag(name = "GitHub Repository Search", description = "Search GitHub repositories and retrieve stored results")
public class GithubSearchController {
    private final GithubSearchService githubSearchService;

    @Operation(
            summary = "Search GitHub repositories",
            description = "Search repositories on GitHub using the GitHub REST API. "
                    + "Results are persisted to the database. Repeated searches update existing records."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Search completed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GithubSearchResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request — missing query or invalid sort value",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "ValidationError",
                                    summary = "400 Validation Error",
                                    value = "{\"timestamp\": \"2026-10-14T10:15:30.123Z\", \"status\": 400, \"error\": \"Validation failed\", \"details\": {\"query\": \"Search query cannot be empty\"}}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "GitHub API rate limit exceeded or access forbidden",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "RateLimitError",
                                    summary = "403 Rate Limit Exceeded",
                                    value = "{\"timestamp\": \"2026-10-14T10:15:30.123Z\", \"status\": 403, \"error\": \"GitHub API rate limit exceeded or access forbidden\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error or database error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "ServerError",
                                    summary = "500 Database or Server Error",
                                    value = "{\"timestamp\": \"2026-10-14T10:15:30.123Z\", \"status\": 500, \"error\": \"A database error occurred. Please try again later.\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "GitHub API returned an unexpected error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "UpstreamError",
                                    summary = "502 GitHub API Error",
                                    value = "{\"timestamp\": \"2026-10-14T10:15:30.123Z\", \"status\": 502, \"error\": \"GitHub API error: 500\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Unable to connect to GitHub API",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "ConnectionError",
                                    summary = "503 Connection Failure",
                                    value = "{\"timestamp\": \"2026-10-14T10:15:30.123Z\", \"status\": 503, \"error\": \"Unable to connect to GitHub API. Please try again later.\"}"
                            )
                    )
            )
    })
    @PostMapping("/search")
    public ResponseEntity<GithubSearchResponse> searchRepositories(
            @Valid @RequestBody GithubSearchRequest request) {

        GithubSearchResponse response =
                githubSearchService.search(request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get stored repositories",
            description = "Retrieve repositories previously saved to the database, "
                    + "with optional filtering by language and minimum stars, and sorting. "
                    + "Default sort is by stars descending."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Repositories retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Repositories.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid sort parameter (must be stars, forks, or updated)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "InvalidSortError",
                                    summary = "400 Invalid Sort Parameter",
                                    value = "{\"timestamp\": \"2026-10-14T10:15:30.123Z\", \"status\": 400, \"error\": \"Invalid sort value. Allowed values: stars, forks, or updated\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error or database error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "DatabaseError",
                                    summary = "500 Database Error",
                                    value = "{\"timestamp\": \"2026-10-14T10:15:30.123Z\", \"status\": 500, \"error\": \"A database error occurred. Please try again later.\"}"
                            )
                    )
            )
    })
    @GetMapping("/repositories")
    public ResponseEntity<List<Repositories>> getStoredRepositories(
            @Parameter(description = "Filter by programming language (case-insensitive)", example = "Java")
            @RequestParam(required = false) String language,

            @Parameter(description = "Minimum number of stars", example = "100")
            @RequestParam(required = false) Integer minStars,

            @Parameter(
                    description = "Sort field: stars, forks, or updated. Default: stars (descending)",
                    schema = @Schema(allowableValues = {"stars", "forks", "updated"}, defaultValue = "stars")
            )
            @RequestParam(defaultValue = "stars") String sort) {

        return ResponseEntity.ok(
                githubSearchService.getStoredRepositories(
                        language, minStars, sort
                )
        );
    }
}
