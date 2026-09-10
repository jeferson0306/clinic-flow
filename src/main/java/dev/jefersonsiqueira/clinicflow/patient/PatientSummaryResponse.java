package dev.jefersonsiqueira.clinicflow.patient;

import dev.jefersonsiqueira.clinicflow.address.Address;
import dev.jefersonsiqueira.clinicflow.common.DocumentMasking;
import java.time.Instant;
import java.util.UUID;

/**
 * Registration data only — no birth date, sex, blood type, allergies,
 * medications, pre-existing conditions, clinical alert, or guardian
 * details. Returned to RECEPCAO instead of {@link PatientResponse}: a
 * receptionist needs to reach a patient and bill a procedure, not read
 * their anamnese, and that boundary belongs in the response contract
 * itself — a field the wire format never carries can't leak through a UI
 * bug the way a merely-hidden one can. See {@link PatientResource} for
 * which role gets which shape.
 */
public record PatientSummaryResponse(
    UUID id, String fullName, String maskedCpf, String email, String phone, Address address, Instant createdAt) {

  public static PatientSummaryResponse from(Patient patient) {
    return new PatientSummaryResponse(
        patient.id,
        patient.fullName,
        DocumentMasking.maskCpf(patient.cpf),
        patient.email,
        patient.phone,
        patient.address,
        patient.createdAt);
  }
}
