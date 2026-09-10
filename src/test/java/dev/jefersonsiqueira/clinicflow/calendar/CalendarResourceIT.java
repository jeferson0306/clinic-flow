package dev.jefersonsiqueira.clinicflow.calendar;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import dev.jefersonsiqueira.clinicflow.validation.brdoc.BrdocClient;
import dev.jefersonsiqueira.clinicflow.validation.brdoc.BrdocValidationResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
// Every write endpoint under test here now requires a role — see the
// RolesAllowed added alongside this. A blanket ADMIN+DOCTOR grant, not a
// real login, because these tests exist to verify business logic, not the
// RBAC boundary itself; AuthResourceIT and AuthorizationIT own that.
@TestSecurity(user = "test-user", roles = {"ADMIN", "DOCTOR"})
class CalendarResourceIT {

  @InjectMock @RestClient BrdocClient brdoc;

  private static String unique(String prefix) {
    return prefix + (System.nanoTime() % 100_000_000L);
  }

  private String patientId;
  private String doctorId;
  private String procedureId;

  @BeforeEach
  void setUpAPatientADoctorAndAProcedure() {
    when(brdoc.validateCpf(anyString()))
        .thenAnswer(inv -> Response.ok(new BrdocValidationResponse(
            true, ((String) inv.getArgument(0)).replaceAll("\\D", ""), "Valid CPF format", null)).build());
    when(brdoc.validateEmail(anyString()))
        .thenAnswer(inv -> Response.ok(
            new BrdocValidationResponse(true, inv.getArgument(0), "Valid email", null)).build());
    when(brdoc.validateCep(anyString()))
        .thenAnswer(inv -> Response.ok(new BrdocValidationResponse(
            true, ((String) inv.getArgument(0)).replaceAll("\\D", ""), "Valid postcode format", null)).build());
    when(brdoc.validateTelephone(anyString()))
        .thenAnswer(inv -> Response.ok(new BrdocValidationResponse(
            true, ((String) inv.getArgument(0)).replaceAll("\\D", ""), "Valid phone", null)).build());

    patientId =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {"fullName":"Ana Souza","cpf":"%s","email":"ana@example.com","phone":"11987654321","birthDate":"1990-05-10","postcode":"01310-200","houseNumber":"123","street":"Avenida Paulista","city":"Sao Paulo","state":"SP"}
                """.formatted(unique("")))
            .post("/v1/patients")
            .jsonPath()
            .getString("id");

    doctorId =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {"fullName":"Dr. Marcos Lima","cpf":"%s","email":"marcos@example.com","specialty":"Cardiology","licenseNumber":"%s"}
                """.formatted(unique(""), unique("LIC-")))
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

  @Test
  void schedulingAnAppointmentRemovesExactlyItsSlotFromAvailability() {
    List<String> before = freeSlotStarts();
    assertThat(before).contains("2026-09-10T11:00:00Z");

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"patientId":"%s","doctorId":"%s","procedureId":"%s","startsAt":"2026-09-10T11:00:00Z"}
            """.formatted(patientId, doctorId, procedureId))
        .when()
        .post("/v1/appointments")
        .then()
        .statusCode(201);

    List<String> after = freeSlotStarts();
    assertThat(after).hasSize(before.size() - 1).doesNotContain("2026-09-10T11:00:00Z");
  }

  private List<String> freeSlotStarts() {
    return given()
        .queryParam("date", "2026-09-10")
        .queryParam("procedureId", procedureId)
        .when()
        .get("/v1/doctors/{doctorId}/availability", doctorId)
        .jsonPath()
        .getList("freeSlots.startsAt");
  }

  @Test
  void returns404ForAnUnknownDoctor() {
    given()
        .queryParam("date", "2026-09-10")
        .queryParam("procedureId", procedureId)
        .when()
        .get("/v1/doctors/{doctorId}/availability", "00000000-0000-0000-0000-000000000000")
        .then()
        .statusCode(404);
  }

  @Test
  void aFullDayHasFewerThanTwentyFourHourlySlots() {
    // Sanity check on the working-hours config actually being applied — a
    // 30-minute procedure across a full calendar day would be 48 slots if the
    // window were midnight-to-midnight instead of the configured 08:00-18:00.
    given()
        .queryParam("date", "2026-09-10")
        .queryParam("procedureId", procedureId)
        .when()
        .get("/v1/doctors/{doctorId}/availability", doctorId)
        .then()
        .statusCode(200)
        .body("freeSlots.size()", lessThan(48));
  }
}
