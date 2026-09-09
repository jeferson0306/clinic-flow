package dev.jefersonsiqueira.clinicflow.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Issues, rotates and revokes refresh tokens — the long-lived credential
 * behind {@code POST /v1/auth/refresh} that lets a session outlive the
 * short-lived access token without asking for a password again.
 *
 * Rotation, not reuse: every successful refresh revokes the token it was
 * called with and issues a brand new one. A refresh token used twice — the
 * signature of a stolen one being replayed alongside the legitimate client
 * that still has it — revokes cleanly the first time and simply fails the
 * second, rather than silently minting endless new tokens off a single
 * compromised value.
 */
@ApplicationScoped
public class RefreshTokenService {

  static final Duration LIFETIME = Duration.ofDays(7);

  private static final int TOKEN_BYTES = 32;

  @Inject RefreshTokenRepository repository;

  private final SecureRandom random = new SecureRandom();

  public record Issued(String rawToken, long expiresInSeconds) {}

  @Transactional
  public Issued issue(UUID userId) {
    return issueFor(userId);
  }

  /**
   * Validates {@code rawToken}, revokes it, and issues a replacement for the
   * same user — the "rotation" half of the class. Throws {@link
   * InvalidRefreshTokenException} for anything wrong with it: not found,
   * expired, or already revoked (including a reused one — see the class
   * javadoc for why that case gets no special treatment).
   */
  @Transactional
  public RotatedResult rotate(String rawToken) {
    RefreshToken existing = repository
        .findByTokenHash(hash(rawToken))
        .filter(t -> t.isUsable(Instant.now()))
        .orElseThrow(InvalidRefreshTokenException::new);

    existing.revokedAt = Instant.now();
    return new RotatedResult(existing.userId, issueFor(existing.userId));
  }

  public record RotatedResult(UUID userId, Issued issued) {}

  /** Revokes a refresh token outright — logout's own use of this class, no replacement issued. */
  @Transactional
  public void revoke(String rawToken) {
    repository.findByTokenHash(hash(rawToken)).ifPresent(t -> {
      if (t.revokedAt == null) {
        t.revokedAt = Instant.now();
      }
    });
  }

  private Issued issueFor(UUID userId) {
    String rawToken = generateRawToken();
    RefreshToken token = new RefreshToken();
    token.userId = userId;
    token.tokenHash = hash(rawToken);
    token.createdAt = Instant.now();
    token.expiresAt = token.createdAt.plus(LIFETIME);
    repository.persist(token);
    return new Issued(rawToken, LIFETIME.toSeconds());
  }

  private String generateRawToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String hash(String rawToken) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is a JDK-guaranteed algorithm (every conforming JVM ships
      // it) — this can only mean something is fundamentally broken with the
      // runtime itself, not a condition normal control flow should model.
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
