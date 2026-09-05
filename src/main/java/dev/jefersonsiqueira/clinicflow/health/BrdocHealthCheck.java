package dev.jefersonsiqueira.clinicflow.health;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Reports brdoc's own reachability at {@code /q/health} — the "database,
 * brdoc, payment provider" gap the README's roadmap named for this phase —
 * without ever failing this service's own readiness because of it.
 *
 * <p>That distinction matters: brdoc runs on Render's free tier and sleeps
 * after 15 minutes idle (see {@code BrdocClient}'s javadoc and
 * AGENTS.md's own note on its cold-start timeout). A brdoc that is merely
 * asleep is not this service being unhealthy — registering a patient still
 * works once brdoc wakes, within its own documented timeout — so this check
 * always reports {@code UP} and puts brdoc's actual reachability in the
 * response data instead of the status. A hard {@code DOWN} here would make
 * Render's own health check (configured against the combined {@code
 * /q/health}) restart this service every time brdoc naps, which is exactly
 * the failure mode this is written to avoid.
 */
@Readiness
public class BrdocHealthCheck implements HealthCheck {

  private static final HttpClient CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

  @ConfigProperty(name = "quarkus.rest-client.brdoc.url")
  String brdocUrl;

  @Override
  public HealthCheckResponse call() {
    var builder = HealthCheckResponse.named("brdoc").up();
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(brdocUrl + "/q/health"))
              .timeout(Duration.ofSeconds(2))
              .GET()
              .build();
      HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
      return builder.withData("reachable", response.statusCode() == 200).withData("status", response.statusCode()).build();
    } catch (Exception e) {
      // Unreachable or asleep — see this class's own javadoc for why that is
      // still reported UP rather than DOWN.
      return builder.withData("reachable", false).withData("error", e.getClass().getSimpleName()).build();
    }
  }
}
