package com.freightfox.githubsearch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Schema(description = "Standard error response returned for all error cases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Schema(description = "ISO-8601 timestamp of when the error occurred", example = "2026-10-14T10:15:30.123Z")
    private String timestamp;

    @Schema(description = "HTTP status code")
    private int status;

    @Schema(description = "Error message describing what went wrong")
    private String error;

    @Schema(description = "Field-level validation errors (present only for validation failures)", nullable = true)
    private Map<String, String> details;
}
