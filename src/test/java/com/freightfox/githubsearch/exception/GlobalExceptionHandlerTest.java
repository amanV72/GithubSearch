package com.freightfox.githubsearch.exception;

import com.freightfox.githubsearch.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    // ── IllegalArgumentException ────────────────────────────────────

    @Test
    @DisplayName("IllegalArgumentException → 400 with message")
    void handleIllegalArgument() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Invalid sort value. Allowed values: stars, forks, or updated"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).contains("Invalid sort value");
        assertThat(response.getBody().getTimestamp()).isNotBlank();
        assertThat(response.getBody().getDetails()).isNull();
    }

    // ── RestClientResponseException (GitHub API errors) ─────────────

    @Test
    @DisplayName("GitHub 403 → FORBIDDEN with rate-limit message")
    void handleGithubApi_403() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", null, null, null);

        ResponseEntity<ErrorResponse> response = handler.handleGithubApiError(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getError())
                .isEqualTo("GitHub API rate limit exceeded or access forbidden");
    }

    @Test
    @DisplayName("GitHub 422 → BAD_REQUEST with validation message")
    void handleGithubApi_422() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable", null, null, null);

        ResponseEntity<ErrorResponse> response = handler.handleGithubApiError(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("GitHub API validation failed");
    }

    @Test
    @DisplayName("GitHub 500 → BAD_GATEWAY with error code in message")
    void handleGithubApi_500() {
        HttpServerErrorException ex = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                null, null, null);

        ResponseEntity<ErrorResponse> response = handler.handleGithubApiError(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().getStatus()).isEqualTo(502);
        assertThat(response.getBody().getError()).isEqualTo("GitHub API error: 500");
    }

    // ── ResourceAccessException (connection failure) ────────────────

    @Test
    @DisplayName("Connection failure → 503 SERVICE_UNAVAILABLE")
    void handleConnectionFailure() {
        ResourceAccessException ex = new ResourceAccessException(
                "I/O error", new IOException("Connection refused"));

        ResponseEntity<ErrorResponse> response = handler.handleConnectionFailure(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getStatus()).isEqualTo(503);
        assertThat(response.getBody().getError())
                .isEqualTo("Unable to connect to GitHub API. Please try again later.");
    }

    // ── DataAccessException (database error) ────────────────────────

    @Test
    @DisplayName("Database error → 500 INTERNAL_SERVER_ERROR")
    void handleDatabaseError() {
        DataAccessResourceFailureException ex =
                new DataAccessResourceFailureException("Connection pool exhausted");

        ResponseEntity<ErrorResponse> response = handler.handleDatabaseError(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError())
                .isEqualTo("A database error occurred. Please try again later.");
    }

    // ── IllegalStateException ───────────────────────────────────────

    @Test
    @DisplayName("IllegalStateException → 500 with original message")
    void handleIllegalState() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalState(
                new IllegalStateException("Invalid response received from GitHub API"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError())
                .isEqualTo("Invalid response received from GitHub API");
    }

    // ── Generic Exception catch-all ─────────────────────────────────

    @Test
    @DisplayName("Unhandled exception → 500 with generic message")
    void handleGenericException() {
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                new RuntimeException("Something unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError())
                .isEqualTo("An unexpected error occurred. Please try again later.");
    }

    // ── Error response structure ────────────────────────────────────

    @Test
    @DisplayName("All error responses include timestamp, status, and error fields")
    void errorResponseStructure() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("test"));

        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTimestamp()).isNotBlank();
        assertThat(body.getStatus()).isPositive();
        assertThat(body.getError()).isNotBlank();
    }
}
