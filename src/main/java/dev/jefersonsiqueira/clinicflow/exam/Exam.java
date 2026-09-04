package dev.jefersonsiqueira.clinicflow.exam;

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
 * Requested against a patient by a doctor; its result is recorded later, by a
 * separate call — an exam usually exists before there is anything to report,
 * and forcing a result at creation would mean a placeholder value standing in
 * for "not done yet".
 */
@Entity
@Table(name = "exams")
public class Exam extends PanacheEntityBase {

  @Id @GeneratedValue @UuidGenerator public UUID id;

  @Column(name = "patient_id", nullable = false)
  public UUID patientId;

  @Column(name = "requested_by_doctor_id", nullable = false)
  public UUID requestedByDoctorId;

  @Column(nullable = false)
  public String type;

  @Column(name = "requested_at", nullable = false)
  public Instant requestedAt;

  /** Null until {@link #resultRecordedAt} is set. */
  @Column
  public String result;

  @Column(name = "result_recorded_at")
  public Instant resultRecordedAt;
}
