package com.freightfox.githubsearch.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class GithubClientConfig {

    @Value("${github.api.base-url:https://api.github.com}")
    private String githubBaseUrl;

    @Value("${github.api.token:}")
    private String githubToken;

    @Bean
    public RestClient.Builder restClient(){
        return RestClient.builder();
    }

    @Bean
    public RestClient githubRestClient(RestClient.Builder builder) {

        RestClient.Builder clientBuilder = builder
                .baseUrl(githubBaseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "githubsearch")
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        "application/vnd.github+json"
                );

        if (githubToken != null && !githubToken.isBlank()) {
            clientBuilder.defaultHeader(
                    HttpHeaders.AUTHORIZATION, "Bearer " + githubToken
            );
        }

        return clientBuilder.build();
    }
}