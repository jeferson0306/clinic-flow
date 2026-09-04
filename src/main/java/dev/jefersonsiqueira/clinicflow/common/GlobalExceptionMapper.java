package dev.jefersonsiqueira.clinicflow.common;

import dev.jefersonsiqueira.clinicflow.appointment.DoubleBookingException;
import dev.jefersonsiqueira.clinicflow.doctor.DuplicateDoctorException;
import dev.jefersonsiqueira.clinicflow.patient.DuplicatePatientException;
import dev.jefersonsiqueira.clinicflow.validation.brdoc.DocumentValidationException;
import io.quarkus.logging.Log;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.NoSuchElementException;

/**
 * One mapper for every domain exception, so a resource method never writes
 * its own try/catch for "this failed, return 4xx" — it just throws and lets
 * this decide the status code.
 *
 * What is logged is deliberately thin: the exception type and the field name,
 * never the value that failed. A rejected CPF or email is still personal data
 * even when invalid, and this service is reachable from a public sandbox —
 * the brdoc lesson applies here just as much as it did there.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

  @Override
  public Response toResponse(Exception exception) {
    return switch (exception) {
      case DocumentValidationException e -> {
        Log.infof("document validation failed: field=%s", e.field());
        yield Response.status(422).entity(new ApiError(e.field(), e.getMessage())).build();
      }
      case DuplicatePatientException e ->
          Response.status(409).entity(new ApiError("cpf", e.getMessage())).build();
      case DuplicateDoctorException e ->
          Response.status(409).entity(new ApiError(e.field(), e.getMessage())).build();
      case DoubleBookingException e ->
          Response.status(409).entity(new ApiError("startsAt", e.getMessage())).build();
      case ConstraintViolationException e -> {
        var first = e.getConstraintViolations().iterator().next();
        yield Response.status(422)
            .entity(new ApiError(first.getPropertyPath().toString(), first.getMessage()))
            .build();
      }
      case NoSuchElementException e -> Response.status(404).build();
      case WebApplicationException e -> e.getResponse();
      default -> {
        Log.error("unhandled exception", exception);
        yield Response.status(500).entity(new ApiError(null, "Internal error")).build();
      }
    };
  }
}
