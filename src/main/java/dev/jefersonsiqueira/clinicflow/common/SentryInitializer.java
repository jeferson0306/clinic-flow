package dev.jefersonsiqueira.clinicflow.common;

import io.quarkus.runtime.StartupEvent;
import io.sentry.Sentry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Initializes the plain Sentry Java SDK once, at startup — see the pom.xml
 * comment next to the dependency for why this is the raw SDK and not the
 * Quarkiverse extension.
 *
 * <p>{@code clinic.sentry.dsn} is unset in %dev and %test on purpose (no
 * {@code SENTRY_DSN} env var there) — the Sentry SDK's own documented
 * behavior for a blank DSN is a safe no-op, exactly the AWS client's
 * {@code clinic.aws.enabled=false} shape, just without needing a flag of
 * its own to express it.
 */
@ApplicationScoped
public class SentryInitializer {

  // Optional<String>, not String — SmallRye Config's built-in String
  // converter treats an empty resolved value as absent rather than "",
  // which makes a plain @ConfigProperty String fail startup validation
  // whenever SENTRY_DSN is unset (every %dev and %test run). Same shape
  // AwsClients.endpointOverride already uses for the same reason.
  @ConfigProperty(name = "clinic.sentry.dsn")
  Optional<String> dsn;

  void onStart(@Observes StartupEvent event) {
    Sentry.init(options -> {
      options.setDsn(dsn.orElse(""));
      options.setTracesSampleRate(0.1);
      // Never send the request body — a rejected CPF, email or phone is
      // still personal data even when invalid, the same brdoc-derived rule
      // GlobalExceptionMapper's own logging already follows.
      options.setSendDefaultPii(false);
    });
  }
}
