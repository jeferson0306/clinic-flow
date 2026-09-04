package dev.jefersonsiqueira.clinicflow.patient;

import dev.jefersonsiqueira.clinicflow.address.Address;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * A UUID primary key, not a sequence: this table is reachable from a public
 * sandbox, and a sequential id lets a visitor enumerate every other patient
 * created by guessing nearby numbers. A UUID does not.
 */
@Entity
@Table(name = "patients")
public class Patient extends PanacheEntityBase {

  @Id @GeneratedValue @UuidGenerator public UUID id;

  @Column(name = "full_name", nullable = false)
  public String fullName;

  /** Normalized by brdoc before this is ever set — digits only, check-digit valid. */
  @Column(name = "cpf", nullable = false, unique = true, length = 11)
  public String cpf;

  @Column(name = "email", nullable = false)
  public String email;

  @Column(name = "phone")
  public String phone;

  @Column(name = "birth_date")
  public LocalDate birthDate;

  @Embedded public Address address;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
