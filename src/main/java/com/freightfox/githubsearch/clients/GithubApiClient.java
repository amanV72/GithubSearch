package com.freightfox.githubsearch.clients;

import com.freightfox.githubsearch.dto.GithubApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class GithubApiClient {

    private final RestClient githubRestClient;

    public GithubApiResponse searchRepositories(
            String query, String sort, String order, int perPage) {
        return githubRestClient.get()
                .uri("/search/repositories?q={q}&sort={sort}&order={order}&per_page={perPage}",
                        query, sort, order, perPage)
                .retrieve()
                .body(GithubApiResponse.class);
    }
}
