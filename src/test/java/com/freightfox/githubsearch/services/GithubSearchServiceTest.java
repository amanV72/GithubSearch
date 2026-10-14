package com.freightfox.githubsearch.services;

import com.freightfox.githubsearch.clients.GithubApiClient;
import com.freightfox.githubsearch.dto.GithubApiResponse;
import com.freightfox.githubsearch.dto.GithubSearchRequest;
import com.freightfox.githubsearch.dto.GithubSearchResponse;
import com.freightfox.githubsearch.dto.Repositories;
import com.freightfox.githubsearch.models.GithubRepositoryEntity;
import com.freightfox.githubsearch.repositories.GithubRepositoryDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubSearchServiceTest {

    @Mock
    private GithubRepositoryDAO githubRepositoryDAO;

    @Mock
    private GithubApiClient githubApiClient;

    @InjectMocks
    private GithubSearchService githubSearchService;

    @Captor
    private ArgumentCaptor<List<GithubRepositoryEntity>> entitiesCaptor;

    // ── Helper builders ─────────────────────────────────────────────

    private GithubApiResponse.Item createItem(Long id, String name, String owner,
                                               String language, int stars, int forks) {
        GithubApiResponse.Owner ownerObj = new GithubApiResponse.Owner(owner);
        return new GithubApiResponse.Item(
                id, name, "Description of " + name, ownerObj,
                language, stars, forks, Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    private GithubApiResponse createApiResponse(List<GithubApiResponse.Item> items) {
        return new GithubApiResponse(items.size(), items);
    }

    private GithubRepositoryEntity createEntity(Long id, String name, String owner,
                                                 String language, int stars, int forks) {
        return new GithubRepositoryEntity(
                id, name, "Description of " + name, owner,
                language, stars, forks, Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    // ── search() tests ──────────────────────────────────────────────

    @Nested
    @DisplayName("search()")
    class SearchTests {

        @Test
        @DisplayName("returns saved repositories on successful search")
        void search_success() {
            GithubSearchRequest request = GithubSearchRequest.builder()
                    .query("spring boot")
                    .build();

            List<GithubApiResponse.Item> items = List.of(
                    createItem(1L, "spring-boot", "spring-projects", "Java", 75000, 12000)
            );
            GithubApiResponse apiResponse = createApiResponse(items);

            when(githubApiClient.searchRepositories("spring boot", "stars", "desc", 30))
                    .thenReturn(apiResponse);

            GithubRepositoryEntity savedEntity = createEntity(
                    1L, "spring-boot", "spring-projects", "Java", 75000, 12000);
            when(githubRepositoryDAO.saveAll(anyList())).thenReturn(List.of(savedEntity));

            GithubSearchResponse response = githubSearchService.search(request);

            assertThat(response.getMessage()).isEqualTo("Repositories fetched and saved successfully");
            assertThat(response.getRepositories()).hasSize(1);

            Repositories repo = response.getRepositories().get(0);
            assertThat(repo.getId()).isEqualTo(1L);
            assertThat(repo.getName()).isEqualTo("spring-boot");
            assertThat(repo.getOwner()).isEqualTo("spring-projects");
            assertThat(repo.getLanguage()).isEqualTo("Java");
            assertThat(repo.getStars()).isEqualTo(75000);
            assertThat(repo.getForks()).isEqualTo(12000);

            verify(githubApiClient).searchRepositories("spring boot", "stars", "desc", 30);
            verify(githubRepositoryDAO).saveAll(anyList());
        }

        @Test
        @DisplayName("appends language filter to query when provided")
        void search_withLanguageFilter() {
            GithubSearchRequest request = GithubSearchRequest.builder()
                    .query("web framework")
                    .language("Python")
                    .build();

            GithubApiResponse apiResponse = createApiResponse(List.of(
                    createItem(2L, "django", "django", "Python", 50000, 8000)
            ));
            when(githubApiClient.searchRepositories("web framework language:Python", "stars", "desc", 30))
                    .thenReturn(apiResponse);
            when(githubRepositoryDAO.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            GithubSearchResponse response = githubSearchService.search(request);

            assertThat(response.getRepositories()).hasSize(1);
            verify(githubApiClient).searchRepositories("web framework language:Python", "stars", "desc", 30);
        }

        @Test
        @DisplayName("uses custom sort when provided")
        void search_withCustomSort() {
            GithubSearchRequest request = GithubSearchRequest.builder()
                    .query("react")
                    .sort("forks")
                    .build();

            GithubApiResponse apiResponse = createApiResponse(List.of(
                    createItem(3L, "react", "facebook", "JavaScript", 200000, 40000)
            ));
            when(githubApiClient.searchRepositories("react", "forks", "desc", 30))
                    .thenReturn(apiResponse);
            when(githubRepositoryDAO.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            githubSearchService.search(request);

            verify(githubApiClient).searchRepositories("react", "forks", "desc", 30);
        }

        @Test
        @DisplayName("defaults to stars sort when sort is null")
        void search_defaultSortWhenNull() {
            GithubSearchRequest request = GithubSearchRequest.builder()
                    .query("java")
                    .sort(null)
                    .build();

            GithubApiResponse apiResponse = createApiResponse(List.of(
                    createItem(4L, "java-repo", "dev", "Java", 100, 10)
            ));
            when(githubApiClient.searchRepositories("java", "stars", "desc", 30))
                    .thenReturn(apiResponse);
            when(githubRepositoryDAO.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            githubSearchService.search(request);

            verify(githubApiClient).searchRepositories("java", "stars", "desc", 30);
        }

        @Test
        @DisplayName("returns empty list with message when no results found")
        void search_emptyResults() {
            GithubSearchRequest request = GithubSearchRequest.builder()
                    .query("nonexistent-repo-xyz123")
                    .build();

            GithubApiResponse apiResponse = new GithubApiResponse(0, List.of());
            when(githubApiClient.searchRepositories("nonexistent-repo-xyz123", "stars", "desc", 30))
                    .thenReturn(apiResponse);

            GithubSearchResponse response = githubSearchService.search(request);

            assertThat(response.getMessage()).isEqualTo("No repositories found");
            assertThat(response.getRepositories()).isEmpty();
            verify(githubRepositoryDAO, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("throws IllegalStateException when API returns null response")
        void search_nullResponse() {
            GithubSearchRequest request = GithubSearchRequest.builder()
                    .query("test")
                    .build();

            when(githubApiClient.searchRepositories("test", "stars", "desc", 30))
                    .thenReturn(null);

            assertThatThrownBy(() -> githubSearchService.search(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Invalid response received from GitHub API");
        }

        @Test
        @DisplayName("throws IllegalStateException when API returns null items")
        void search_nullItems() {
            GithubSearchRequest request = GithubSearchRequest.builder()
                    .query("test")
                    .build();

            GithubApiResponse apiResponse = new GithubApiResponse(null, null);
            when(githubApiClient.searchRepositories("test", "stars", "desc", 30))
                    .thenReturn(apiResponse);

            assertThatThrownBy(() -> githubSearchService.search(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Invalid response received from GitHub API");
        }

        @Test
        @DisplayName("handles item with null owner gracefully")
        void search_nullOwner() {
            GithubSearchRequest request = GithubSearchRequest.builder()
                    .query("test")
                    .build();

            GithubApiResponse.Item item = new GithubApiResponse.Item(
                    5L, "orphan-repo", "desc", null, "Go", 50, 5,
                    Instant.parse("2026-01-01T00:00:00Z")
            );
            GithubApiResponse apiResponse = createApiResponse(List.of(item));
            when(githubApiClient.searchRepositories("test", "stars", "desc", 30))
                    .thenReturn(apiResponse);
            when(githubRepositoryDAO.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            GithubSearchResponse response = githubSearchService.search(request);

            assertThat(response.getRepositories().get(0).getOwner()).isNull();
        }

        @Test
        @DisplayName("persists all entities to database")
        void search_persistsEntities() {
            GithubSearchRequest request = GithubSearchRequest.builder()
                    .query("test")
                    .build();

            List<GithubApiResponse.Item> items = List.of(
                    createItem(10L, "repo-a", "user1", "Java", 100, 10),
                    createItem(20L, "repo-b", "user2", "Python", 200, 20)
            );
            GithubApiResponse apiResponse = createApiResponse(items);
            when(githubApiClient.searchRepositories("test", "stars", "desc", 30))
                    .thenReturn(apiResponse);
            when(githubRepositoryDAO.saveAll(entitiesCaptor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            githubSearchService.search(request);

            List<GithubRepositoryEntity> captured = entitiesCaptor.getValue();
            assertThat(captured).hasSize(2);
            assertThat(captured.get(0).getId()).isEqualTo(10L);
            assertThat(captured.get(1).getId()).isEqualTo(20L);
        }
    }

    // ── getStoredRepositories() tests ───────────────────────────────

    @Nested
    @DisplayName("getStoredRepositories()")
    class GetStoredRepositoriesTests {

        @Test
        @DisplayName("returns all repositories with default stars sort")
        void getStored_defaultSort() {
            List<GithubRepositoryEntity> entities = List.of(
                    createEntity(1L, "repo-a", "user1", "Java", 500, 50)
            );
            when(githubRepositoryDAO.findFiltered(isNull(), isNull(),
                    eq(Sort.by(Sort.Direction.DESC, "stars"))))
                    .thenReturn(entities);

            List<Repositories> result = githubSearchService.getStoredRepositories(
                    null, null, "stars");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("repo-a");
            verify(githubRepositoryDAO).findFiltered(isNull(), isNull(),
                    eq(Sort.by(Sort.Direction.DESC, "stars")));
        }

        @Test
        @DisplayName("passes language filter to DAO")
        void getStored_withLanguageFilter() {
            when(githubRepositoryDAO.findFiltered(eq("Java"), isNull(),
                    eq(Sort.by(Sort.Direction.DESC, "stars"))))
                    .thenReturn(List.of());

            githubSearchService.getStoredRepositories("Java", null, "stars");

            verify(githubRepositoryDAO).findFiltered(eq("Java"), isNull(),
                    eq(Sort.by(Sort.Direction.DESC, "stars")));
        }

        @Test
        @DisplayName("passes minStars filter to DAO")
        void getStored_withMinStarsFilter() {
            when(githubRepositoryDAO.findFiltered(isNull(), eq(100),
                    eq(Sort.by(Sort.Direction.DESC, "stars"))))
                    .thenReturn(List.of());

            githubSearchService.getStoredRepositories(null, 100, "stars");

            verify(githubRepositoryDAO).findFiltered(isNull(), eq(100),
                    eq(Sort.by(Sort.Direction.DESC, "stars")));
        }

        @Test
        @DisplayName("sorts by forks when sort=forks")
        void getStored_sortByForks() {
            when(githubRepositoryDAO.findFiltered(isNull(), isNull(),
                    eq(Sort.by(Sort.Direction.DESC, "forks"))))
                    .thenReturn(List.of());

            githubSearchService.getStoredRepositories(null, null, "forks");

            verify(githubRepositoryDAO).findFiltered(isNull(), isNull(),
                    eq(Sort.by(Sort.Direction.DESC, "forks")));
        }

        @Test
        @DisplayName("sorts by lastUpdated when sort=updated")
        void getStored_sortByUpdated() {
            when(githubRepositoryDAO.findFiltered(isNull(), isNull(),
                    eq(Sort.by(Sort.Direction.DESC, "lastUpdated"))))
                    .thenReturn(List.of());

            githubSearchService.getStoredRepositories(null, null, "updated");

            verify(githubRepositoryDAO).findFiltered(isNull(), isNull(),
                    eq(Sort.by(Sort.Direction.DESC, "lastUpdated")));
        }

        @Test
        @DisplayName("defaults to stars sort when sortBy is null")
        void getStored_nullSort() {
            when(githubRepositoryDAO.findFiltered(isNull(), isNull(),
                    eq(Sort.by(Sort.Direction.DESC, "stars"))))
                    .thenReturn(List.of());

            githubSearchService.getStoredRepositories(null, null, null);

            verify(githubRepositoryDAO).findFiltered(isNull(), isNull(),
                    eq(Sort.by(Sort.Direction.DESC, "stars")));
        }

        @Test
        @DisplayName("defaults to stars sort when sortBy is blank")
        void getStored_blankSort() {
            when(githubRepositoryDAO.findFiltered(isNull(), isNull(),
                    eq(Sort.by(Sort.Direction.DESC, "stars"))))
                    .thenReturn(List.of());

            githubSearchService.getStoredRepositories(null, null, "  ");

            verify(githubRepositoryDAO).findFiltered(isNull(), isNull(),
                    eq(Sort.by(Sort.Direction.DESC, "stars")));
        }

        @Test
        @DisplayName("throws IllegalArgumentException for invalid sort value")
        void getStored_invalidSort() {
            assertThatThrownBy(() ->
                    githubSearchService.getStoredRepositories(null, null, "popularity"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid sort value");
        }

        @Test
        @DisplayName("normalizes blank language to null")
        void getStored_blankLanguageNormalized() {
            when(githubRepositoryDAO.findFiltered(isNull(), isNull(),
                    any(Sort.class)))
                    .thenReturn(List.of());

            githubSearchService.getStoredRepositories("  ", null, "stars");

            verify(githubRepositoryDAO).findFiltered(isNull(), isNull(), any(Sort.class));
        }

        @Test
        @DisplayName("maps entity fields to DTO correctly")
        void getStored_mappingCorrectness() {
            Instant now = Instant.parse("2026-06-15T12:00:00Z");
            GithubRepositoryEntity entity = new GithubRepositoryEntity(
                    42L, "my-repo", "A great repo", "octocat",
                    "Rust", 999, 88, now
            );
            when(githubRepositoryDAO.findFiltered(isNull(), isNull(), any(Sort.class)))
                    .thenReturn(List.of(entity));

            List<Repositories> result = githubSearchService.getStoredRepositories(
                    null, null, "stars");

            assertThat(result).hasSize(1);
            Repositories dto = result.get(0);
            assertThat(dto.getId()).isEqualTo(42L);
            assertThat(dto.getName()).isEqualTo("my-repo");
            assertThat(dto.getDescription()).isEqualTo("A great repo");
            assertThat(dto.getOwner()).isEqualTo("octocat");
            assertThat(dto.getLanguage()).isEqualTo("Rust");
            assertThat(dto.getStars()).isEqualTo(999);
            assertThat(dto.getForks()).isEqualTo(88);
            assertThat(dto.getLastUpdated()).isEqualTo(now);
        }
    }
}
