package dev.jefersonsiqueira.clinicflow.common;

import io.opentelemetry.api.trace.Span;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Echoes the request's own OpenTelemetry trace id back as {@code X-Trace-Id}.
 *
 * A caller reporting a problem can hand back this one value, and it is the
 * same id every log line for that request was written under — no separate
 * request-id to generate, propagate and keep in sync with tracing
 * independently; the trace id already is the request's identity.
 */
@Provider
public class TraceIdResponseFilter implements ContainerResponseFilter {

  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response) {
    String traceId = Span.current().getSpanContext().getTraceId();
    response.getHeaders().add("X-Trace-Id", traceId);
  }
}
