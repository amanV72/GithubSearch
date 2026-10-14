
package com.freightfox.githubsearch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubApiResponse {

    @JsonProperty("total_count")
    private Integer totalCount;

    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        private Long id;
        private String name;
        private String description;
        private Owner owner;
        private String language;

        @JsonProperty("stargazers_count")
        private Integer stars;

        @JsonProperty("forks_count")
        private Integer forks;

        @JsonProperty("updated_at")
        private Instant lastUpdated;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Owner {

        private String login;
    }
}
