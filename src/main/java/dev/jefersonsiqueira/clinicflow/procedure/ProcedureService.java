package dev.jefersonsiqueira.clinicflow.procedure;

import dev.jefersonsiqueira.clinicflow.common.ResourceInUseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;

@ApplicationScoped
public class ProcedureService {

  private static final String FOREIGN_KEY_VIOLATION_SQLSTATE = "23503";

  @Inject ProcedureRepository procedures;

  @Transactional
  public Procedure create(CreateProcedureRequest request) {
    Procedure procedure = new Procedure();
    procedure.name = request.name().trim();
    procedure.durationMinutes = request.durationMinutes();
    procedure.priceCents = request.priceCents();
    procedures.persist(procedure);
    return procedure;
  }

  public List<Procedure> listAll() {
    return procedures.listAll();
  }

  public Procedure findById(UUID id) {
    return procedures.findByIdOptional(id).orElseThrow(NoSuchElementException::new);
  }

  @Transactional
  public Procedure update(UUID id, CreateProcedureRequest request) {
    Procedure procedure = findById(id);
    procedure.name = request.name().trim();
    procedure.durationMinutes = request.durationMinutes();
    procedure.priceCents = request.priceCents();
    return procedure;
  }

  @Transactional
  public void delete(UUID id) {
    Procedure procedure = findById(id);
    try {
      procedures.delete(procedure);
      procedures.getEntityManager().flush();
    } catch (ConstraintViolationException e) {
      if (FOREIGN_KEY_VIOLATION_SQLSTATE.equals(e.getSQLState())) {
        throw new ResourceInUseException("This procedure has appointments on record and cannot be deleted");
      }
      throw e;
    }
  }
}
