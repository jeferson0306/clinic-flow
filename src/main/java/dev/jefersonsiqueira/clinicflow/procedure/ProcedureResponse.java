package dev.jefersonsiqueira.clinicflow.procedure;

import java.util.UUID;

public record ProcedureResponse(UUID id, String name, int durationMinutes, long priceCents) {
  public static ProcedureResponse from(Procedure procedure) {
    return new ProcedureResponse(
        procedure.id, procedure.name, procedure.durationMinutes, procedure.priceCents);
  }
}
