package dev.jefersonsiqueira.clinicflow.audit;

import dev.jefersonsiqueira.clinicflow.ratelimit.ClientAddressResolver;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Records who accessed patient/exam data, what they did, and when — the
 * LGPD access trail this session's audit asked for. A single filter rather
 * than a {@code record(...)} call scattered across {@code PatientResource}
 * and {@code ExamResource}: the mapping from "HTTP verb on this path" to
 * "what happened" is mechanical and identical for both resources, so it is
 * written once here instead of twice there.
 *
 * Scoped to {@code /v1/patients} and {@code /v1/exams} only — the same two
 * resources this session's security audit flagged as carrying real PHI
 * (full name, CPF, birth date, exam results). Every other resource in this
 * API (doctors, procedures, appointments) is operational data, not the kind
 * LGPD access-logging exists for, so logging it here would just be noise a
 * real audit trail has to be filtered back out of.
 *
 * Only successful (2xx) responses are recorded — a 401/403/404 never
 * touched the record in question, so there is nothing to attribute an
 * action to. {@code @Blocking}: this writes to Postgres via
 * {@link AuditLogService}, so it must not run on the Vert.x event loop the
 * way a response filter does by default.
 */
@Provider
@Blocking
public class AuditLogFilter implements ContainerResponseFilter {

  @Inject AuditLogService auditLog;
  @Inject ClientAddressResolver addressResolver;
  @Inject JsonWebToken jwt;

  @Context HttpHeaders headers;
  @Context HttpServerRequest vertxRequest;

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if (responseContext.getStatus() < 200 || responseContext.getStatus() >= 300) {
      return;
    }

    String path = requestContext.getUriInfo().getPath();
    String resourceType = resourceTypeFor(path);
    if (resourceType == null) {
      return;
    }

    AuditAction action = actionFor(requestContext.getMethod());
    if (action == null) {
      return;
    }

    String cfConnectingIp = headers == null ? null : headers.getHeaderString("CF-Connecting-IP");
    String remoteAddress = vertxRequest == null ? null : vertxRequest.remoteAddress().hostAddress();
    String ip = addressResolver.resolve(cfConnectingIp, remoteAddress);

    String actorEmail = jwt.getName();
    String actorRole = jwt.getGroups() == null || jwt.getGroups().isEmpty() ? null : jwt.getGroups().iterator().next();

    auditLog.record(actorEmail, actorRole, action, resourceType, resourceIdFor(path), ip);
  }

  private static String resourceTypeFor(String path) {
    String normalized = path.startsWith("/") ? path.substring(1) : path;
    if (normalized.startsWith("v1/patients")) {
      return "PATIENT";
    }
    if (normalized.startsWith("v1/exams")) {
      return "EXAM";
    }
    return null;
  }

  private static AuditAction actionFor(String httpMethod) {
    return switch (httpMethod) {
      case "GET" -> AuditAction.VIEWED;
      case "POST" -> AuditAction.CREATED;
      case "PUT", "PATCH" -> AuditAction.UPDATED;
      case "DELETE" -> AuditAction.DELETED;
      default -> null;
    };
  }

  /** The last path segment, if it's a UUID — a list endpoint's path has no id and yields null. */
  private static UUID resourceIdFor(String path) {
    String[] segments = path.split("/");
    if (segments.length == 0) {
      return null;
    }
    try {
      return UUID.fromString(segments[segments.length - 1]);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
