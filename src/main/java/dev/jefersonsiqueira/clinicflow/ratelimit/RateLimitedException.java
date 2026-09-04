package dev.jefersonsiqueira.clinicflow.ratelimit;

/** This client address has exceeded its request budget. */
public class RateLimitedException extends RuntimeException {
  public RateLimitedException() {
    super("Too many requests from this address");
  }
}
