package dev.jefersonsiqueira.clinicflow.procedure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateProcedureRequest(
    @NotBlank String name, @Positive int durationMinutes, @Positive long priceCents) {}
