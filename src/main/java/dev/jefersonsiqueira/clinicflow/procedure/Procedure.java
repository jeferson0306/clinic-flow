package dev.jefersonsiqueira.clinicflow.procedure;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * A clinical service that can be booked — a consultation, an exam, a
 * procedure — with the duration an appointment for it reserves and the price
 * billing will eventually charge. No document field here, so nothing in this
 * module ever calls brdoc.
 */
@Entity
@Table(name = "procedures")
public class Procedure extends PanacheEntityBase {

  @Id @GeneratedValue @UuidGenerator public UUID id;

  @Column(nullable = false)
  public String name;

  @Column(name = "duration_minutes", nullable = false)
  public int durationMinutes;

  /** Cents, not a floating-point currency amount — the usual reason. */
  @Column(name = "price_cents", nullable = false)
  public long priceCents;
}
