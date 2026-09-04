package dev.jefersonsiqueira.clinicflow.appointment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import dev.jefersonsiqueira.clinicflow.validation.brdoc.BrdocClient;
import dev.jefersonsiqueira.clinicflow.validation.brdoc.BrdocValidationResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The one thing worth an integration test here is the exclusion constraint
 * itself — that cannot be exercised against a mock, so this creates a real
 * patient, doctor and procedure through their own endpoints (brdoc mocked,
 * same as their own test classes) and schedules real appointments against
 * them.
 */
@QuarkusTest
// Every write endpoint under test here now requires a role — see the
// RolesAllowed added alongside this. A blanket ADMIN+DOCTOR grant, not a
// real login, because these tests exist to verify business logic, not the
// RBAC boundary itself; AuthResourceIT and AuthorizationIT own that.
@TestSecurity(user = "test-user", roles = {"ADMIN", "DOCTOR"})
class AppointmentResourceIT {

  @InjectMock @RestClient BrdocClient brdoc;

  private String patientId;
  private String doctorId;
  private String procedureId;

  // brdoc is mocked in this class — see below — so nothing here needs a CPF
  // that would actually pass a check-digit algorithm, only one that is
  // unique. It has to be: @BeforeEach runs fresh before each of this class's
  // three test methods, and the CPF/licence-number uniqueness constraints are
  // real, on a Postgres container shared for the whole test run — a fixed
  // literal collided with itself on the second test method, and separately
  // with PatientResourceTest's and DoctorResourceTest's own fixtures. The
  // same lesson their tests already had to learn.
  private static String unique(String prefix) {
    return prefix + (System.nanoTime() % 100_000_000L);
  }

  @BeforeEach
  void setUpAClinicWithOnePatientOneDoctorAndOneProcedure() {
    String patientCpf = unique("");
    String doctorCpf = unique("");
    String licenseNumber = unique("LIC-");
    when(brdoc.validateCpf(anyString()))
        .thenAnswer(inv -> Response.ok(new BrdocValidationResponse(
            true, ((String) inv.getArgument(0)).replaceAll("\\D", ""), "Valid CPF format", null)).build());
    when(brdoc.validateEmail(anyString()))
        .thenAnswer(inv -> Response.ok(
            new BrdocValidationResponse(true, inv.getArgument(0), "Valid email", null)).build());
    when(brdoc.validateCep(anyString()))
        .thenAnswer(inv -> Response.ok(new BrdocValidationResponse(
            true, ((String) inv.getArgument(0)).replaceAll("\\D", ""), "Valid postcode format", null)).build());

    patientId =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {"fullName":"Ana Souza","cpf":"%s","email":"ana@example.com","postcode":"01310-200"}
                """.formatted(patientCpf))
            .post("/v1/patients")
            .jsonPath()
            .getString("id");

    doctorId =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {"fullName":"Dr. Marcos Lima","cpf":"%s","email":"marcos@example.com","specialty":"Cardiology","licenseNumber":"%s"}
                """.formatted(doctorCpf, licenseNumber))
            .post("/v1/doctors")
            .jsonPath()
            .getString("id");

    procedureId =
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name":"Consultation","durationMinutes":30,"priceCents":15000}
                """)
            .post("/v1/procedures")
            .jsonPath()
            .getString("id");
  }

  private String schedule(String startsAt) {
    return """
        {"patientId":"%s","doctorId":"%s","procedureId":"%s","startsAt":"%s"}
        """.formatted(patientId, doctorId, procedureId, startsAt);
  }

  @Test
  void rejectsAnOverlappingAppointmentForTheSameDoctor() {
    Instant tenAM = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

    given()
        .contentType(ContentType.JSON)
        .body(schedule(tenAM.toString()))
        .when()
        .post("/v1/appointments")
        .then()
        .statusCode(201);

    // Starts 15 minutes into the first appointment's 30-minute slot — a real
    // overlap, not an adjacent booking.
    given()
        .contentType(ContentType.JSON)
        .body(schedule(tenAM.plus(15, ChronoUnit.MINUTES).toString()))
        .when()
        .post("/v1/appointments")
        .then()
        .statusCode(409)
        .body("field", is("startsAt"));
  }

  @Test
  void acceptsABackToBackAppointmentThatDoesNotOverlap() {
    Instant tenAM = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

    given()
        .contentType(ContentType.JSON)
        .body(schedule(tenAM.toString()))
        .when()
        .post("/v1/appointments")
        .then()
        .statusCode(201);

    // Starts exactly when the 30-minute first appointment ends: adjacent, not
    // overlapping — [10:00, 10:30) and [10:30, 11:00) share no instant.
    given()
        .contentType(ContentType.JSON)
        .body(schedule(tenAM.plus(30, ChronoUnit.MINUTES).toString()))
        .when()
        .post("/v1/appointments")
        .then()
        .statusCode(201);
  }

  @Test
  void cancellingFreesTheSlotForAnotherBooking() {
    Instant tenAM = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

    String appointmentId =
        given()
            .contentType(ContentType.JSON)
            .body(schedule(tenAM.toString()))
            .post("/v1/appointments")
            .jsonPath()
            .getString("id");

    given().post("/v1/appointments/" + appointmentId + "/cancel").then().statusCode(200);

    // The same slot, same doctor — would 409 if the cancelled row still
    // counted, since the exclusion constraint is scoped to SCHEDULED only.
    given()
        .contentType(ContentType.JSON)
        .body(schedule(tenAM.toString()))
        .when()
        .post("/v1/appointments")
        .then()
        .statusCode(201);
  }
}
