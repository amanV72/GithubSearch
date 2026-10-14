package com.freightfox.githubsearch.services;

import com.freightfox.githubsearch.clients.GithubApiClient;
import com.freightfox.githubsearch.dto.GithubApiResponse;
import com.freightfox.githubsearch.dto.GithubSearchRequest;
import com.freightfox.githubsearch.dto.GithubSearchResponse;
import com.freightfox.githubsearch.dto.Repositories;
import com.freightfox.githubsearch.models.GithubRepositoryEntity;
import com.freightfox.githubsearch.repositories.GithubRepositoryDAO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GithubSearchService {
    private final GithubRepositoryDAO githubRepositoryDAO;
    private final GithubApiClient githubApiClient;

    @Transactional
    public GithubSearchResponse search(GithubSearchRequest request) {

        String query = request.getQuery().trim();

        if (request.getLanguage() != null
                && !request.getLanguage().isBlank()) {
            query += " language:" + request.getLanguage().trim();
        }

        String sort = request.getSort() == null
                || request.getSort().isBlank()
                ? "stars"
                : request.getSort().trim().toLowerCase(Locale.ROOT);

        GithubApiResponse response =
                githubApiClient.searchRepositories(
                        query, sort, "desc", 30
                );

        if (response == null || response.getItems() == null) {
            throw new IllegalStateException(
                    "Invalid response received from GitHub API"
            );
        }

        if (response.getItems().isEmpty()) {
            return GithubSearchResponse.builder()
                    .message("No repositories found")
                    .repositories(List.of())
                    .build();
        }

        List<GithubRepositoryEntity> entities = response.getItems()
                .stream()
                .map(item -> {
                    GithubRepositoryEntity repo = new GithubRepositoryEntity();
                    repo.setId(item.getId());
                    repo.setName(item.getName());
                    repo.setDescription(item.getDescription());
                    repo.setOwner(item.getOwner() != null
                            ? item.getOwner().getLogin() : null);
                    repo.setLanguage(item.getLanguage());
                    repo.setStars(item.getStars());
                    repo.setForks(item.getForks());
                    repo.setLastUpdated(item.getLastUpdated());
                    return repo;
                })
                .toList();

        List<GithubRepositoryEntity> savedRepositories =
                githubRepositoryDAO.saveAll(entities);

        List<Repositories> repositories = savedRepositories.stream()
                .map(this::toDto)
                .toList();

        return GithubSearchResponse.builder()
                .message("Repositories fetched and saved successfully")
                .repositories(repositories)
                .build();
    }

    public List<Repositories> getStoredRepositories(
            String language,
            Integer minStars,
            String sortBy) {

        String sortField = (sortBy == null || sortBy.isBlank())
                ? "stars"
                : sortBy.trim().toLowerCase(Locale.ROOT);

        sortField = switch (sortField) {
            case "stars" -> "stars";
            case "forks" -> "forks";
            case "updated" -> "lastUpdated";
            default -> throw new IllegalArgumentException(
                    "Invalid sort value. Allowed values: stars, forks, or updated"
            );
        };

        String normalizedLanguage = (language != null && !language.isBlank())
                ? language.trim()
                : null;

        List<GithubRepositoryEntity> results =
                githubRepositoryDAO.findFiltered(
                        normalizedLanguage,
                        minStars,
                        Sort.by(Sort.Direction.DESC, sortField)
                );

        return results.stream()
                .map(this::toDto)
                .toList();
    }

    private Repositories toDto(GithubRepositoryEntity repo) {
        return Repositories.builder()
                .id(repo.getId())
                .name(repo.getName())
                .description(repo.getDescription())
                .owner(repo.getOwner())
                .language(repo.getLanguage())
                .stars(repo.getStars())
                .forks(repo.getForks())
                .lastUpdated(repo.getLastUpdated())
                .build();
    }
}
