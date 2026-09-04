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

  public LoginResponse login(LoginRequest request) {
    User user = users.findByUsername(request.username()).orElseThrow(InvalidCredentialsException::new);

    // Bcrypt's own comparison, not String.equals on a hash: constant-time,
    // and the only correct way to check a value against a salted hash.
    if (!BcryptUtil.matches(request.password(), user.passwordHash)) {
      throw new InvalidCredentialsException();
    }

    String token =
        Jwt.issuer(ISSUER)
            .upn(user.username)
            .groups(user.role.name())
            .expiresIn(TOKEN_LIFETIME)
            .sign();

    return new LoginResponse(token, TOKEN_LIFETIME.toSeconds(), user.role);
  }
}
