package dev.jefersonsiqueira.clinicflow.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;

@ApplicationScoped
public class AuthService {

  /** Matches mp.jwt.issuer, so a token this class issues is one SmallRye JWT will also accept. */
  static final String ISSUER = "https://clinic-flow";

  // Short-lived on purpose: this is the token every request actually carries
  // and checks, so its window is how long a leaked one stays useful. The
  // session itself lasts as long as RefreshTokenService.LIFETIME (7 days) —
  // POST /v1/auth/refresh trades a still-valid refresh token for a new one
  // of these without asking for a password again.
  static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);

  @Inject UserRepository users;
  @Inject PasswordPolicy passwordPolicy;
  @Inject RefreshTokenService refreshTokens;

  public LoginResponse login(LoginRequest request) {
    // Case-insensitive on purpose — email addresses are, by the spec the
    // rest of the world actually follows, and "Admin@X" failing to match a
    // stored "admin@x" is a real login bug users hit constantly, not an
    // edge case worth ignoring.
    String normalizedEmail = request.email().trim().toLowerCase();
    User user = users.findByEmail(normalizedEmail).orElseThrow(InvalidCredentialsException::new);

    // Bcrypt's own comparison, not String.equals on a hash: constant-time,
    // and the only correct way to check a value against a salted hash.
    if (!BcryptUtil.matches(request.password(), user.passwordHash)) {
      throw new InvalidCredentialsException();
    }

    return issueSession(user);
  }

  /**
   * Rotates a still-valid refresh token for a fresh access+refresh pair —
   * see {@link RefreshTokenService}'s own javadoc for why every call revokes
   * the token it was handed and never reuses it.
   */
  public LoginResponse refresh(String rawRefreshToken) {
    RefreshTokenService.RotatedResult rotated = refreshTokens.rotate(rawRefreshToken);
    User user = users.findByIdOptional(rotated.userId()).orElseThrow(InvalidRefreshTokenException::new);
    return new LoginResponse(
        issueAccessToken(user), TOKEN_LIFETIME.toSeconds(), user.role,
        rotated.issued().rawToken(), rotated.issued().expiresInSeconds());
  }

  /** Revokes a refresh token outright — the signed-out device's session cannot be silently renewed anymore. */
  public void logout(String rawRefreshToken) {
    refreshTokens.revoke(rawRefreshToken);
  }

  private LoginResponse issueSession(User user) {
    RefreshTokenService.Issued refreshToken = refreshTokens.issue(user.id);
    return new LoginResponse(
        issueAccessToken(user), TOKEN_LIFETIME.toSeconds(), user.role,
        refreshToken.rawToken(), refreshToken.expiresInSeconds());
  }

  private String issueAccessToken(User user) {
    return Jwt.issuer(ISSUER).upn(user.email).groups(user.role.name()).expiresIn(TOKEN_LIFETIME).sign();
  }

  @jakarta.transaction.Transactional
  public void changePassword(String currentUserEmail, ChangePasswordRequest request) {
    User user = users.findByEmail(currentUserEmail).orElseThrow(InvalidCredentialsException::new);

    if (!BcryptUtil.matches(request.currentPassword(), user.passwordHash)) {
      throw new InvalidCredentialsException();
    }

    passwordPolicy.validate(request.newPassword());
    user.passwordHash = BcryptUtil.bcryptHash(request.newPassword());
  }
}
