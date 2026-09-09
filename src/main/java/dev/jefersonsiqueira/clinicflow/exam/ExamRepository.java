package dev.jefersonsiqueira.clinicflow.exam;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ExamRepository implements PanacheRepositoryBase<Exam, UUID> {

  /** Every exam for one patient, newest first — MeResource's own source, never a caller-supplied filter. */
  public List<Exam> findByPatientId(UUID patientId) {
    return list("patientId", Sort.by("requestedAt").descending(), patientId);
  }
}
