package dev.jefersonsiqueira.clinicflow.auth;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * Only {@code tokenHash} — the SHA-256 of the raw token — is ever stored;
 * the raw value exists only in memory, once, on the response that hands it
 * to the client. See {@link RefreshTokenService} for issuance and rotation.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends PanacheEntityBase {

  @Id @GeneratedValue @UuidGenerator public UUID id;

  @Column(name = "user_id", nullable = false)
  public UUID userId;

  @Column(name = "token_hash", nullable = false, unique = true)
  public String tokenHash;

  @Column(name = "expires_at", nullable = false)
  public Instant expiresAt;

  @Column(name = "revoked_at")
  public Instant revokedAt;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  public boolean isUsable(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }
}
