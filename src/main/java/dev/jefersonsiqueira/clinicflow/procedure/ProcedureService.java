package dev.jefersonsiqueira.clinicflow.procedure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@ApplicationScoped
public class ProcedureService {

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
}
