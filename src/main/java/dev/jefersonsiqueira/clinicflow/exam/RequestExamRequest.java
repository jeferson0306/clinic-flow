package dev.jefersonsiqueira.clinicflow.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequestExamRequest(
    @NotNull UUID patientId, @NotNull UUID requestedByDoctorId, @NotBlank String type) {}
