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

  static final Duration TOKEN_LIFETIME = Duration.ofHours(8);

  @Inject UserRepository users;
  @Inject PasswordPolicy passwordPolicy;

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

    String token =
        Jwt.issuer(ISSUER)
            .upn(user.email)
            .groups(user.role.name())
            .expiresIn(TOKEN_LIFETIME)
            .sign();

    return new LoginResponse(token, TOKEN_LIFETIME.toSeconds(), user.role);
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
