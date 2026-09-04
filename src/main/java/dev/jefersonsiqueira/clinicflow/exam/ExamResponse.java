package dev.jefersonsiqueira.clinicflow.exam;

import java.time.Instant;
import java.util.UUID;

public record ExamResponse(
    UUID id,
    UUID patientId,
    UUID requestedByDoctorId,
    String type,
    Instant requestedAt,
    String result,
    Instant resultRecordedAt) {

  public static ExamResponse from(Exam exam) {
    return new ExamResponse(
        exam.id,
        exam.patientId,
        exam.requestedByDoctorId,
        exam.type,
        exam.requestedAt,
        exam.result,
        exam.resultRecordedAt);
  }
}
