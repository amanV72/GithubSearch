package com.freightfox.githubsearch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "Response returned after searching GitHub repositories")
@Builder
@Data
public class GithubSearchResponse {

    @Schema(description = "Status message", example = "Repositories fetched and saved successfully")
    private String message;

    @Schema(description = "List of repositories returned from the search")
    private List<Repositories> repositories;
}
