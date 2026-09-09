package dev.jefersonsiqueira.clinicflow.patient;

import dev.jefersonsiqueira.clinicflow.address.Address;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  // Everything below is optional clinical/legal-guardian data, added
  // alongside the original registration fields — see
  // V12__expand_patient_clinical_data.sql for why every column is nullable.

  @Column(name = "social_name")
  public String socialName;

  @Column(name = "mother_name")
  public String motherName;

  @Enumerated(EnumType.STRING)
  @Column(name = "sex")
  public Sex sex;

  @Enumerated(EnumType.STRING)
  @Column(name = "blood_type")
  public BloodType bloodType;

  @Column(name = "allergies")
  public String allergies;

  @Column(name = "continuous_medications")
  public String continuousMedications;

  @Column(name = "pre_existing_conditions")
  public String preExistingConditions;

  /** Shown as a visual warning on the patient's record in the frontend when non-null. */
  @Column(name = "clinical_alert")
  public String clinicalAlert;

  @Column(name = "guardian_name")
  public String guardianName;

  @Column(name = "guardian_cpf", length = 11)
  public String guardianCpf;

  @Enumerated(EnumType.STRING)
  @Column(name = "guardian_relationship")
  public GuardianRelationship guardianRelationship;

  @Column(name = "guardian_phone")
  public String guardianPhone;
}
