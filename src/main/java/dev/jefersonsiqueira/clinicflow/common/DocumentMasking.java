package dev.jefersonsiqueira.clinicflow.common;

/**
 * Masks a normalized document value to its last two characters, keeping the
 * rest as asterisks. Used wherever a CPF is returned from a public endpoint —
 * {@code PatientResponse}, {@code DoctorResponse} — because this system is
 * reachable from a public sandbox and a CPF identifies a real person outside
 * it, in full, regardless of whether it was ever checked against anything
 * this system considers sensitive.
 */
public final class DocumentMasking {

  private DocumentMasking() {}

  public static String maskCpf(String cpf) {
    return "*".repeat(cpf.length() - 2) + cpf.substring(cpf.length() - 2);
  }
}
