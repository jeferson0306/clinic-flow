package dev.jefersonsiqueira.clinicflow.ratelimit;

import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

/**
 * Throttles by client address — see {@link ClientAddressResolver} for what
 * that actually means — and lets {@code GlobalExceptionMapper} decide the
 * response shape, the same as every other rejection in this API.
 *
 * {@code @PreMatching}: this runs before JAX-RS resolves a resource method,
 * so a throttled request never reaches JPA, brdoc, or anything else this
 * budget exists to protect — the check is the very first thing that happens
 * to it.
 *
 * {@code /q/health} is exempt: a monitor that gets throttled reports an
 * outage that is not happening, and a health check is the one caller whose
 * rate is already known and harmless.
 */
@Provider
@PreMatching
public class RateLimitFilter implements ContainerRequestFilter {

  @Inject RateLimiterRegistry registry;
  @Inject ClientAddressResolver addressResolver;

  @Context HttpHeaders headers;
  @Context HttpServerRequest vertxRequest;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String path = requestContext.getUriInfo().getPath();
    if (path.startsWith("q/health") || path.startsWith("/q/health")) {
      return;
    }

    String cfConnectingIp = headers == null ? null : headers.getHeaderString("CF-Connecting-IP");
    String remoteAddress = vertxRequest == null ? null : vertxRequest.remoteAddress().hostAddress();
    String address = addressResolver.resolve(cfConnectingIp, remoteAddress);

    if (!registry.allow(address)) {
      throw new RateLimitedException();
    }
  }
}
