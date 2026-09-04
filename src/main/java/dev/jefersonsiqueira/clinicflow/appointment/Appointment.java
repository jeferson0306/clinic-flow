package dev.jefersonsiqueira.clinicflow.appointment;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * The one rule this table cannot be allowed to break — a doctor double-booked
 * into two places at once — is enforced by Postgres itself, not by this
 * class: see V5's {@code EXCLUDE USING gist} constraint. Checking "is this
 * doctor free" with a SELECT before the INSERT would still race under
 * concurrent booking; the database is the only thing that sees every
 * candidate write at once.
 */
@Entity
@Table(name = "appointments")
public class Appointment extends PanacheEntityBase {

  public enum Status {
    SCHEDULED,
    CANCELLED
  }

  @Id @GeneratedValue @UuidGenerator public UUID id;

  @Column(name = "patient_id", nullable = false)
  public UUID patientId;

  @Column(name = "doctor_id", nullable = false)
  public UUID doctorId;

  @Column(name = "procedure_id", nullable = false)
  public UUID procedureId;

  @Column(name = "starts_at", nullable = false)
  public Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  public Instant endsAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Status status;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
