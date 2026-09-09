package dev.jefersonsiqueira.clinicflow.auth;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepositoryBase<RefreshToken, UUID> {

  public Optional<RefreshToken> findByTokenHash(String tokenHash) {
    return find("tokenHash", tokenHash).firstResultOptional();
  }
}
