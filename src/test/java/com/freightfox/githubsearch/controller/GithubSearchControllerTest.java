package com.freightfox.githubsearch.controller;

import com.freightfox.githubsearch.dto.GithubSearchRequest;
import com.freightfox.githubsearch.dto.GithubSearchResponse;
import com.freightfox.githubsearch.dto.Repositories;
import com.freightfox.githubsearch.exception.GlobalExceptionHandler;
import com.freightfox.githubsearch.services.GithubSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GithubSearchController.class)
@Import(GlobalExceptionHandler.class)
class GithubSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GithubSearchService githubSearchService;

    // ── Helper builders ─────────────────────────────────────────────

    private Repositories sampleRepo(Long id, String name, String language, int stars) {
        return Repositories.builder()
                .id(id).name(name).owner("owner")
                .description("desc").language(language)
                .stars(stars).forks(10)
                .lastUpdated(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    // ── POST /api/github/search ─────────────────────────────────────

    @Nested
    @DisplayName("POST /api/github/search")
    class SearchEndpointTests {

        @Test
        @DisplayName("200 — successful search returns JSON response")
        void search_success() throws Exception {
            GithubSearchResponse serviceResponse = GithubSearchResponse.builder()
                    .message("Repositories fetched and saved successfully")
                    .repositories(List.of(sampleRepo(1L, "spring-boot", "Java", 75000)))
                    .build();

            when(githubSearchService.search(any(GithubSearchRequest.class)))
                    .thenReturn(serviceResponse);

            mockMvc.perform(post("/api/github/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"query": "spring boot", "language": "Java", "sort": "stars"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Repositories fetched and saved successfully"))
                    .andExpect(jsonPath("$.repositories", hasSize(1)))
                    .andExpect(jsonPath("$.repositories[0].name").value("spring-boot"))
                    .andExpect(jsonPath("$.repositories[0].stars").value(75000));
        }

        @Test
        @DisplayName("200 — search with no results")
        void search_noResults() throws Exception {
            GithubSearchResponse serviceResponse = GithubSearchResponse.builder()
                    .message("No repositories found")
                    .repositories(List.of())
                    .build();

            when(githubSearchService.search(any(GithubSearchRequest.class)))
                    .thenReturn(serviceResponse);

            mockMvc.perform(post("/api/github/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"query": "nonexistent-xyz-123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("No repositories found"))
                    .andExpect(jsonPath("$.repositories", hasSize(0)));
        }

        @Test
        @DisplayName("400 — blank query returns validation error")
        void search_blankQuery() throws Exception {
            mockMvc.perform(post("/api/github/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"query": ""}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation failed"))
                    .andExpect(jsonPath("$.details.query").value("Search query cannot be empty"));

            verify(githubSearchService, never()).search(any());
        }

        @Test
        @DisplayName("400 — missing query returns validation error")
        void search_missingQuery() throws Exception {
            mockMvc.perform(post("/api/github/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"language": "Java"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation failed"))
                    .andExpect(jsonPath("$.details.query").exists());

            verify(githubSearchService, never()).search(any());
        }

        @Test
        @DisplayName("400 — invalid sort value returns validation error")
        void search_invalidSort() throws Exception {
            mockMvc.perform(post("/api/github/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"query": "java", "sort": "popularity"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation failed"))
                    .andExpect(jsonPath("$.details.sort").exists());

            verify(githubSearchService, never()).search(any());
        }

        @Test
        @DisplayName("200 — search without optional fields succeeds")
        void search_onlyRequiredFields() throws Exception {
            GithubSearchResponse serviceResponse = GithubSearchResponse.builder()
                    .message("Repositories fetched and saved successfully")
                    .repositories(List.of(sampleRepo(1L, "repo", "Java", 100)))
                    .build();
            when(githubSearchService.search(any())).thenReturn(serviceResponse);

            mockMvc.perform(post("/api/github/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"query": "java"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.repositories", hasSize(1)));
        }

        @Test
        @DisplayName("error response has consistent JSON structure")
        void search_errorResponseStructure() throws Exception {
            mockMvc.perform(post("/api/github/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"query": ""}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").isNumber())
                    .andExpect(jsonPath("$.error").isString())
                    .andExpect(jsonPath("$.details").isMap());
        }
    }

    // ── GET /api/github/repositories ────────────────────────────────

    @Nested
    @DisplayName("GET /api/github/repositories")
    class GetRepositoriesEndpointTests {

        @Test
        @DisplayName("200 — returns stored repositories with default params")
        void getRepos_default() throws Exception {
            List<Repositories> repos = List.of(
                    sampleRepo(1L, "repo-a", "Java", 500),
                    sampleRepo(2L, "repo-b", "Python", 300)
            );
            when(githubSearchService.getStoredRepositories(null, null, "stars"))
                    .thenReturn(repos);

            mockMvc.perform(get("/api/github/repositories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("repo-a"))
                    .andExpect(jsonPath("$[1].name").value("repo-b"));
        }

        @Test
        @DisplayName("200 — filters by language")
        void getRepos_withLanguage() throws Exception {
            when(githubSearchService.getStoredRepositories("Java", null, "stars"))
                    .thenReturn(List.of(sampleRepo(1L, "spring-boot", "Java", 75000)));

            mockMvc.perform(get("/api/github/repositories")
                            .param("language", "Java"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].language").value("Java"));
        }

        @Test
        @DisplayName("200 — filters by minStars")
        void getRepos_withMinStars() throws Exception {
            when(githubSearchService.getStoredRepositories(null, 1000, "stars"))
                    .thenReturn(List.of(sampleRepo(1L, "popular", "Go", 5000)));

            mockMvc.perform(get("/api/github/repositories")
                            .param("minStars", "1000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].stars").value(5000));
        }

        @Test
        @DisplayName("200 — sorts by forks")
        void getRepos_sortByForks() throws Exception {
            when(githubSearchService.getStoredRepositories(null, null, "forks"))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/github/repositories")
                            .param("sort", "forks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(githubSearchService).getStoredRepositories(null, null, "forks");
        }

        @Test
        @DisplayName("200 — combines all filters")
        void getRepos_allFilters() throws Exception {
            when(githubSearchService.getStoredRepositories("Rust", 500, "updated"))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/github/repositories")
                            .param("language", "Rust")
                            .param("minStars", "500")
                            .param("sort", "updated"))
                    .andExpect(status().isOk());

            verify(githubSearchService).getStoredRepositories("Rust", 500, "updated");
        }

        @Test
        @DisplayName("400 — invalid sort returns error JSON")
        void getRepos_invalidSort() throws Exception {
            when(githubSearchService.getStoredRepositories(null, null, "popularity"))
                    .thenThrow(new IllegalArgumentException(
                            "Invalid sort value. Allowed values: stars, forks, or updated"));

            mockMvc.perform(get("/api/github/repositories")
                            .param("sort", "popularity"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value(
                            "Invalid sort value. Allowed values: stars, forks, or updated"));
        }

        @Test
        @DisplayName("200 — returns empty list when no repositories stored")
        void getRepos_emptyDatabase() throws Exception {
            when(githubSearchService.getStoredRepositories(null, null, "stars"))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/github/repositories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
