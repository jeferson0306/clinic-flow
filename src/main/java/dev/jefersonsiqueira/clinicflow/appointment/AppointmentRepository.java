package dev.jefersonsiqueira.clinicflow.appointment;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class AppointmentRepository implements PanacheRepositoryBase<Appointment, UUID> {}
