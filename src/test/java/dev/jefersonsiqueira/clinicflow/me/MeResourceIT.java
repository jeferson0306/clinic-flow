package dev.jefersonsiqueira.clinicflow.me;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import dev.jefersonsiqueira.clinicflow.patient.PatientRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Proves the actual security property MeResource exists for: a PACIENTE
 * token only ever sees the patient named in its own {@code patientId}
 * claim, never one it asks for by a different id. There is no id parameter
 * on any of these routes to even attempt that with — see MeResource's own
 * javadoc — so what this really tests is that two different tokens each
 * see their own record and nothing of the other's.
 */
@QuarkusTest
class MeResourceIT {

  @Inject PatientRepository patients;

  @Test
  @TestSecurity(user = "patient-a", roles = "PACIENTE")
  @JwtSecurity(claims = @Claim(key = "patientId", value = "11111111-1111-1111-1111-111111111111"))
  void seesOnlyItsOwnPatientRecord() {
    UUID patientId = seedPatient("11111111-1111-1111-1111-111111111111", "Patient A");

    given().when().get("/v1/me/patient").then().statusCode(200).body("id", is(patientId.toString())).body("fullName", is("Patient A"));
  }

  @Test
  @TestSecurity(user = "patient-a", roles = "PACIENTE")
  @JwtSecurity(claims = @Claim(key = "patientId", value = "22222222-2222-2222-2222-222222222222"))
  void neverSeesAnotherPatientsRecordEvenWithADifferentClaim() {
    seedPatient("11111111-1111-1111-1111-111111111111", "Patient A");
    seedPatient("22222222-2222-2222-2222-222222222222", "Patient B");

    // This token's own claim is patient B's id — it gets B, never A, and
    // there is no request parameter through which it could ask for A.
    given().when().get("/v1/me/patient").then().statusCode(200).body("fullName", is("Patient B"));
  }

  @Test
  @TestSecurity(user = "patient-c", roles = "PACIENTE")
  @JwtSecurity(claims = @Claim(key = "patientId", value = "33333333-3333-3333-3333-333333333333"))
  void appointmentsAndExamsAreEmptyForAPatientWithNone() {
    seedPatient("33333333-3333-3333-3333-333333333333", "Patient C");

    given().when().get("/v1/me/appointments").then().statusCode(200).body("size()", is(0));
    given().when().get("/v1/me/exams").then().statusCode(200).body("size()", is(0));
  }

  /**
   * A native insert, not {@code PatientRepository.persist()}: {@code
   * Patient.id} is {@code @GeneratedValue}, and Hibernate treats an entity
   * handed to {@code persist()} with that field already set as detached
   * rather than transient — the exact "Detached entity passed to persist"
   * failure this session already hit once building AuditLogFilterIT. A
   * fixed id is the whole point here (it has to match the {@code @Claim}
   * value above, which is a compile-time literal), so this test seeds the
   * row directly rather than fighting the generator for it.
   */
  private UUID seedPatient(String id, String fullName) {
    UUID patientId = UUID.fromString(id);
    EntityManager em = patients.getEntityManager();
    QuarkusTransaction.requiringNew().run(() -> {
      if (patients.findByIdOptional(patientId).isPresent()) {
        return;
      }
      em.createNativeQuery(
              "insert into patients (id, full_name, cpf, email, created_at) values (?1, ?2, ?3, ?4, now())")
          .setParameter(1, patientId)
          .setParameter(2, fullName)
          .setParameter(3, id.replace("-", "").substring(0, 11))
          .setParameter(4, fullName.toLowerCase().replace(" ", ".") + "@example.com")
          .executeUpdate();
    });
    return patientId;
  }
}
