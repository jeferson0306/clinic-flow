package dev.jefersonsiqueira.clinicflow.common;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.opentelemetry.api.trace.Span;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;

/**
 * Handles a bad enum value in a request body — e.g. {@code "sex":
 * "NOT_A_REAL_VALUE"} — with the same {@link ApiError} shape every other
 * rejection in this API returns.
 *
 * A separate provider, not a case in {@link GlobalExceptionMapper}'s own
 * switch: JAX-RS routes an exception to the *most specific* registered
 * {@code ExceptionMapper<T>}, and Quarkus's own REST Jackson extension
 * already registers one for this exact exception type. A case inside {@code
 * GlobalExceptionMapper implements ExceptionMapper<Exception>} would never
 * actually run — {@code Exception} loses to the more specific match every
 * time, regardless of what the switch inside it says. Found live: this
 * exact case was written there once, verified working end to end against a
 * real request, and returned the framework's own generic body instead.
 */
@Provider
public class InvalidFormatExceptionMapper implements ExceptionMapper<InvalidFormatException> {

  @Context UriInfo uriInfo;

  @Inject RecentErrorsLog recentErrors;

  @Override
  public Response toResponse(InvalidFormatException exception) {
    String field = exception.getPath().isEmpty()
        ? null
        : exception.getPath().get(exception.getPath().size() - 1).getFieldName();
    String path = uriInfo == null ? null : uriInfo.getPath();
    String traceId = Span.current().getSpanContext().getTraceId();

    var body = new ApiError(field, "invalid value for field", ErrorCategory.VALIDATION, traceId, Instant.now(), path);
    recentErrors.record(new RecentErrorsLog.Entry(Instant.now(), 422, "InvalidFormatException", path, traceId));

    return Response.status(422).entity(body).build();
  }
}
