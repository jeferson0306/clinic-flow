package dev.jefersonsiqueira.clinicflow.procedure;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateProcedureRequest(
    @NotBlank @Size(max = 120) String name,
    @Positive @Max(480) int durationMinutes,
    @Positive @Max(10_000_000) long priceCents) {}
