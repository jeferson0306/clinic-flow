package dev.jefersonsiqueira.clinicflow.exam;

import dev.jefersonsiqueira.clinicflow.doctor.DoctorService;
import dev.jefersonsiqueira.clinicflow.notification.ExamReportPublisher;
import dev.jefersonsiqueira.clinicflow.patient.PatientService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@ApplicationScoped
public class ExamService {

  @Inject ExamRepository exams;
  @Inject PatientService patients;
  @Inject DoctorService doctors;
  @Inject ExamReportPublisher reportPublisher;

  @Transactional
  public Exam request(RequestExamRequest request) {
    // Reuses each service's own findById rather than a second existence check:
    // a patient or doctor id that does not exist is the same "not found" a
    // direct fetch would report, and NoSuchElementException already maps to
    // 404 — no new exception type earns its keep here.
    patients.findById(request.patientId());
    doctors.findById(request.requestedByDoctorId());

    Exam exam = new Exam();
    exam.patientId = request.patientId();
    exam.requestedByDoctorId = request.requestedByDoctorId();
    exam.type = request.type().trim();
    exam.requestedAt = Instant.now();
    exams.persist(exam);
    return exam;
  }

  @Transactional
  public Exam recordResult(UUID examId, RecordExamResultRequest request) {
    Exam exam = findById(examId);
    exam.result = request.result().trim();
    exam.resultRecordedAt = Instant.now();
    // Archival + notification, not part of what makes this write correct —
    // see ExamReportPublisher's own javadoc for why it never throws back
    // into this method.
    reportPublisher.publish(exam);
    return exam;
  }

  public Exam findById(UUID id) {
    return exams.findByIdOptional(id).orElseThrow(NoSuchElementException::new);
  }

  /** Every exam, newest first — see PatientService.listAll's javadoc for why no pagination yet. */
  public List<Exam> listAll() {
    return exams.listAll(io.quarkus.panache.common.Sort.by("requestedAt").descending());
  }
}
