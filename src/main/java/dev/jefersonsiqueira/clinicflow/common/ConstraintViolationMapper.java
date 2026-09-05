package dev.jefersonsiqueira.clinicflow.common;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * quarkus-hibernate-validator registers its own mapper for {@code
 * ValidationException} to produce RESTEasy Reactive's {@code
 * {title,status,violations}} shape for every {@code @Valid} body-parameter
 * failure. JAX-RS resolves an exception to the mapper whose declared type is
 * closest in the class hierarchy to the exception's actual class — and
 * {@code ValidationException} (two steps up from the exception RESTEasy
 * Reactive actually throws) is closer than {@link GlobalExceptionMapper}'s
 * {@code Exception}, so that mapper always won, silently, for every {@code
 * @Valid} violation on this API. Confirmed against the real deployed
 * backend: {@code POST /v1/patients} with an invalid {@code fullName} or a
 * future {@code birthDate} returned RESTEasy's shape, not this API's {@link
 * ApiError} — which defeated every frontend lookup keyed on {@code field},
 * with no exception, error log, or test failure anywhere to point at it.
 * Registering explicitly for {@code ConstraintViolationException} — one
 * step closer than {@code ValidationException} — wins the match instead;
 * this class does nothing but hand the exception to the same handling every
 * other exception on this API already gets.
 */
@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

  @Inject GlobalExceptionMapper delegate;

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    return delegate.toResponse(exception);
  }
}
