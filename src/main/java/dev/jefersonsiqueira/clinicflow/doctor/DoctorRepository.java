package dev.jefersonsiqueira.clinicflow.doctor;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class DoctorRepository implements PanacheRepositoryBase<Doctor, UUID> {

  public boolean existsByCpf(String cpf) {
    return find("cpf", cpf).firstResultOptional().isPresent();
  }

  public boolean existsByLicenseNumber(String licenseNumber) {
    return find("licenseNumber", licenseNumber).firstResultOptional().isPresent();
  }
}
