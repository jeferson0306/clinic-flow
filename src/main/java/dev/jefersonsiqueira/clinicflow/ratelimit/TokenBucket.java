package dev.jefersonsiqueira.clinicflow.ratelimit;

/**
 * A classic token bucket: tokens refill continuously up to a capacity, and a
 * request is allowed only if one is available to spend. Burst-tolerant by
 * construction — a caller that has been idle can spend its whole capacity at
 * once, which is the point of a burst allowance rather than a flaw in it.
 *
 * Not thread-safe on its own — {@link RateLimiterRegistry} is what
 * synchronizes access, once per bucket, so this stays a plain value holder.
 */
final class TokenBucket {

  private final double capacity;
  private final double refillPerSecond;
  private double tokens;
  private long lastRefillNanos;

  TokenBucket(double capacity, double refillPerSecond) {
    this.capacity = capacity;
    this.refillPerSecond = refillPerSecond;
    this.tokens = capacity;
    this.lastRefillNanos = System.nanoTime();
  }

  boolean tryConsume() {
    refill();
    boolean hasToken = tokens >= 1.0;
    tokens = hasToken ? tokens - 1.0 : tokens;
    return hasToken;
  }

  private void refill() {
    long now = System.nanoTime();
    double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
    tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
    lastRefillNanos = now;
  }
}
