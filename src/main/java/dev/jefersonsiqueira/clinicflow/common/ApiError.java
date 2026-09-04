package dev.jefersonsiqueira.clinicflow.common;

import java.time.Instant;

/**
 * The one error shape every endpoint returns, whatever went wrong.
 *
 * {@code traceId} duplicates {@code X-Trace-Id} (see TraceIdResponseFilter)
 * on purpose: a caller working from a saved response body — a bug report, a
 * failed-request log a frontend kept — has the same correlation id without
 * needing the headers to have been captured too.
 */
public record ApiError(
    String field, String message, ErrorCategory category, String traceId, Instant timestamp, String path) {}
