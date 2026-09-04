package dev.jefersonsiqueira.clinicflow.doctor;

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
 * A UUID id for the same reason as {@code Patient}: this table is reachable
 * from a public sandbox, and a sequential id would let a visitor enumerate
 * every doctor by guessing nearby numbers.
 */
@Entity
@Table(name = "doctors")
public class Doctor extends PanacheEntityBase {

  @Id @GeneratedValue @UuidGenerator public UUID id;

  @Column(name = "full_name", nullable = false)
  public String fullName;

  /** Normalized by brdoc before this is ever set — digits only, check-digit valid. */
  @Column(name = "cpf", nullable = false, unique = true, length = 11)
  public String cpf;

  @Column(name = "email", nullable = false)
  public String email;

  @Column(name = "specialty", nullable = false)
  public String specialty;

  /**
   * The CRM — the state medical council registration number, e.g.
   * "123456-SP". Unlike a CPF this has no public check-digit algorithm to
   * validate against, so it is stored as given, trimmed and upper-cased, and
   * only its presence and uniqueness are enforced.
   */
  @Column(name = "license_number", nullable = false, unique = true)
  public String licenseNumber;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
