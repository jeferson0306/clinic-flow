package dev.jefersonsiqueira.clinicflow.patient;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/**
 * A repository rather than the active-record {@code PanacheEntity} pattern:
 * {@link PatientService} depends on this interface, not on a static method on
 * {@link Patient}, which is what makes the service testable without a
 * database.
 */
@ApplicationScoped
public class PatientRepository implements PanacheRepositoryBase<Patient, UUID> {

  public boolean existsByCpf(String cpf) {
    return find("cpf", cpf).firstResultOptional().isPresent();
  }
}
