package dev.jefersonsiqueira.clinicflow.ratelimit;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The one place this service decides what a caller's address actually is —
 * used by both {@link RateLimitFilter} and
 * {@code GlobalExceptionMapper}'s log line, so the two never disagree about
 * who made a request.
 *
 * A header like {@code X-Forwarded-For} can be set by anyone, including the
 * caller itself; trusting it blindly lets a client claim to be whoever it
 * wants and evade both logging and rate limiting. It is only safe to read
 * once a specific front door is known to strip and overwrite it — mirrors
 * brdoc's own {@code TRUSTED_PLATFORM}, down to the env var name, so the two
 * Render-deployed services in this portfolio are configured the same way.
 * Render fronts with Cloudflare, which sets {@code CF-Connecting-IP} to the
 * real client on every request and cannot be overridden by one.
 *
 * Unset, nothing is trusted: the connection's own remote address is used,
 * which is honest (it is never forgeable) even though it collapses every
 * caller behind Render's own edge to one address.
 */
@ApplicationScoped
public class ClientAddressResolver {

  @ConfigProperty(name = "trusted-platform", defaultValue = "none")
  String trustedPlatform;

  /**
   * @param cfConnectingIpHeader the raw {@code CF-Connecting-IP} header value, if any —
   *     the caller reads it however is natural in its own context
   *     ({@code ContainerRequestContext.getHeaderString}, {@code HttpHeaders.getHeaderString}).
   * @param remoteAddress the connection's own peer address, used whenever the platform is not
   *     trusted or the header is absent.
   */
  public String resolve(String cfConnectingIpHeader, String remoteAddress) {
    if ("cloudflare".equalsIgnoreCase(trustedPlatform)
        && cfConnectingIpHeader != null
        && !cfConnectingIpHeader.isBlank()) {
      return cfConnectingIpHeader;
    }
    return remoteAddress != null ? remoteAddress : "unknown";
  }
}
