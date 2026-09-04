package dev.jefersonsiqueira.clinicflow.validation.brdoc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mirrors {@code models.ValidationResponse} in the brdoc Go service exactly —
 * field names and all, since Jackson binds by JSON key and brdoc's JSON is
 * snake_case. Only the fields this client actually reads are declared;
 * {@code @JsonIgnoreProperties} keeps an upstream addition from breaking
 * deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrdocValidationResponse(
    @JsonProperty("is_valid") boolean valid,
    @JsonProperty("parameter_value") String normalizedValue,
    @JsonProperty("message") String message,
    @JsonProperty("error_code") String errorCode) {}
