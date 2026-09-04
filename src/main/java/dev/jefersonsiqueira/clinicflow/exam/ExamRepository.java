package dev.jefersonsiqueira.clinicflow.exam;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class ExamRepository implements PanacheRepositoryBase<Exam, UUID> {}
