package com.freightfox.githubsearch;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
		info = @Info(
				title = "GitHub Repository Search API",
				version = "1.0",
				description = "API to search GitHub repositories and retrieve saved repositories with filtering and sorting."
		)
)
public class GithubsearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(GithubsearchApplication.class, args);
	}

}
