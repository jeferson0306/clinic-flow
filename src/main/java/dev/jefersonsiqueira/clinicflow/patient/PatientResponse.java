package dev.jefersonsiqueira.clinicflow.patient;

import dev.jefersonsiqueira.clinicflow.address.Address;
import dev.jefersonsiqueira.clinicflow.common.DocumentMasking;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The CPF comes back masked, keeping only the last two digits. This is a
 * public sandbox — anyone can register, and anyone with an id can fetch a
 * record back — and a full CPF is the one field here that identifies a real
 * person outside this system. Nothing downstream needs it in full: brdoc
 * already validated it before it was stored.
 */
public record PatientResponse(
    UUID id,
    String fullName,
    String maskedCpf,
    String email,
    String phone,
    LocalDate birthDate,
    Address address,
    Instant createdAt) {

  public static PatientResponse from(Patient patient) {
    return new PatientResponse(
        patient.id,
        patient.fullName,
        DocumentMasking.maskCpf(patient.cpf),
        patient.email,
        patient.phone,
        patient.birthDate,
        patient.address,
        patient.createdAt);
  }
}
