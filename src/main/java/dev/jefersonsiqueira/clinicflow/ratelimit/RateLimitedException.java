package dev.jefersonsiqueira.clinicflow.ratelimit;

/** This client address has exceeded its request budget. */
public class RateLimitedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public RateLimitedException() {
    super("Too many requests from this address");
  }
}
