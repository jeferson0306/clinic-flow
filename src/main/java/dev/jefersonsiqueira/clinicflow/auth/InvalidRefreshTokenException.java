package dev.jefersonsiqueira.clinicflow.auth;

/** Not found, expired, or already revoked — every case reported the same way, for the same enumeration reason as {@link InvalidCredentialsException}. */
public class InvalidRefreshTokenException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InvalidRefreshTokenException() {
    super("Invalid or expired refresh token");
  }
}
