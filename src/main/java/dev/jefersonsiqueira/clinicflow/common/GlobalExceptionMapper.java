package dev.jefersonsiqueira.clinicflow.common;

import dev.jefersonsiqueira.clinicflow.appointment.DoubleBookingException;
import dev.jefersonsiqueira.clinicflow.doctor.DuplicateDoctorException;
import dev.jefersonsiqueira.clinicflow.patient.DuplicatePatientException;
import dev.jefersonsiqueira.clinicflow.ratelimit.ClientAddressResolver;
import dev.jefersonsiqueira.clinicflow.ratelimit.RateLimitedException;
import dev.jefersonsiqueira.clinicflow.validation.brdoc.DocumentValidationException;
import io.opentelemetry.api.trace.Span;
import io.quarkus.logging.Log;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.NoSuchElementException;

/**
 * One mapper for every domain exception, so a resource method never writes
 * its own try/catch for "this failed, return 4xx" — it just throws and lets
 * this decide the status code, the {@link ErrorCategory}, and what reaches
 * the log.
 *
 * What is logged is deliberately thin on values: the exception type, the
 * field name, the category, the caller's address and the path — never the
 * value that failed. A rejected CPF or email is still personal data even
 * when invalid, and this service is reachable from a public sandbox — the
 * brdoc lesson applies here just as much as it did there. Every line carries
 * the request's OpenTelemetry trace id via the log format in
 * application.properties, which is what actually makes "who hit this, when,
 * with what" answerable without logging the request body itself.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

  @Context UriInfo uriInfo;
  @Context HttpHeaders headers;
  @Context HttpServerRequest vertxRequest;

  @Inject ClientAddressResolver addressResolver;

  @Override
  public Response toResponse(Exception exception) {
    Response response =
        switch (exception) {
          case DocumentValidationException e -> error(422, ErrorCategory.VALIDATION, e.field(), e.getMessage());
          case DuplicatePatientException e -> error(409, ErrorCategory.CONFLICT, "cpf", e.getMessage());
          case DuplicateDoctorException e -> error(409, ErrorCategory.CONFLICT, e.field(), e.getMessage());
          case DoubleBookingException e -> error(409, ErrorCategory.CONFLICT, "startsAt", e.getMessage());
          case RateLimitedException e ->
              Response.fromResponse(error(429, ErrorCategory.RATE_LIMITED, null, e.getMessage()))
                  .header("Retry-After", "1")
                  .build();
          case ConstraintViolationException e -> {
            var first = e.getConstraintViolations().iterator().next();
            yield error(422, ErrorCategory.VALIDATION, first.getPropertyPath().toString(), first.getMessage());
          }
          case NoSuchElementException e -> error(404, ErrorCategory.NOT_FOUND, null, "Not found");
          case WebApplicationException e -> e.getResponse();
          default -> {
            Log.errorf(exception, "unhandled exception: category=%s requester=%s", ErrorCategory.SYSTEM, requester());
            yield error(500, ErrorCategory.SYSTEM, null, "Internal error");
          }
        };
    logOutcome(exception, response.getStatus());
    return response;
  }

  private Response error(int status, ErrorCategory category, String field, String message) {
    var body =
        new ApiError(field, message, category, Span.current().getSpanContext().getTraceId(), Instant.now(), path());
    return Response.status(status).entity(body).build();
  }

  // Deliberately not logged for the SYSTEM branch above — that one already logs the
  // full exception, and logging it twice would just duplicate the stack trace.
  private void logOutcome(Exception exception, int status) {
    if (status == 500) {
      return;
    }
    Log.infof(
        "request rejected: status=%d exceptionType=%s requester=%s path=%s",
        status, exception.getClass().getSimpleName(), requester(), path());
  }

  private String path() {
    return uriInfo == null ? null : uriInfo.getPath();
  }

  private String requester() {
    String cfConnectingIp = headers == null ? null : headers.getHeaderString("CF-Connecting-IP");
    String remoteAddress = vertxRequest == null ? null : vertxRequest.remoteAddress().hostAddress();
    return addressResolver.resolve(cfConnectingIp, remoteAddress);
  }
}
