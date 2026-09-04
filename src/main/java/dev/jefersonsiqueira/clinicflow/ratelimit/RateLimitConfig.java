package dev.jefersonsiqueira.clinicflow.ratelimit;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.time.Duration;

/**
 * No authenticated identity to key on yet (Phase 5), so the limit is set
 * generously rather than tightly — the same trade-off brdoc's own rate
 * limiter documents: everyone behind one address (an office, a carrier NAT)
 * shares a bucket, so it has to be loose enough that they are not punished
 * for sharing an address, while still being tight enough that a script
 * hammering a public sandbox is.
 */
@ConfigMapping(prefix = "clinic.rate-limit")
public interface RateLimitConfig {

  @WithDefault("20")
  double requestsPerSecond();

  @WithDefault("60")
  double burst();

  /** How long a client is remembered after its last request — without eviction the registry grows forever. */
  @WithDefault("PT10M")
  Duration idleBefore();
}
