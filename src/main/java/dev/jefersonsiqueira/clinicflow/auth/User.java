package dev.jefersonsiqueira.clinicflow.auth;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {

  @Id @GeneratedValue @UuidGenerator public UUID id;

  /** The login identifier — a real email shape, validated at the request layer, not just a label. */
  @Column(nullable = false, unique = true)
  public String email;

  /** Bcrypt, via BcryptUtil — never a value this class or anything else compares by equality. */
  @Column(name = "password_hash", nullable = false)
  public String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Role role;

  /** Set only for role PACIENTE — which patient this login belongs to. Null for every staff role. */
  @Column(name = "patient_id")
  public UUID patientId;
}
