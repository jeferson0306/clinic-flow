package dev.jefersonsiqueira.clinicflow.exam;

import jakarta.validation.constraints.NotBlank;

public record RecordExamResultRequest(@NotBlank String result) {}
