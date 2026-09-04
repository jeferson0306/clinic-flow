package dev.jefersonsiqueira.clinicflow.exam;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

/**
 * The one integration test class this module never had, until now — Phases
 * 0-2 covered every other resource. brdoc mocked, same pattern as the
 * others; a real patient and doctor created through their own endpoints so
 * an exam has something real to reference.
 */
@QuarkusTest
@TestSecurity(user = "test-user", roles = {"ADMIN", "DOCTOR"})
class ExamResourceIT {

  @InjectMock @RestClient BrdocClient brdoc;

  private String patientId;
  private String doctorId;

  private static String unique(String prefix) {
    return prefix + (System.nanoTime() % 100_000_000L);
  }

  @BeforeEach
  void setUpAPatientAndADoctor() {
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
  }

  @Test
  void requestsAnExamWithNoResultYet() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"patientId":"%s","requestedByDoctorId":"%s","type":"Complete blood count"}
            """.formatted(patientId, doctorId))
        .when()
        .post("/v1/exams")
        .then()
        .statusCode(201)
        .body("type", is("Complete blood count"))
        .body("result", nullValue())
        .body("resultRecordedAt", nullValue());
  }

  @Test
  void returns404ForAnUnknownPatient() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"patientId":"00000000-0000-0000-0000-000000000000","requestedByDoctorId":"%s","type":"X-ray"}
            """.formatted(doctorId))
        .when()
        .post("/v1/exams")
        .then()
        .statusCode(404);
  }

  @Test
  void recordsAResultAfterTheExamWasRequested() {
    String examId =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {"patientId":"%s","requestedByDoctorId":"%s","type":"Complete blood count"}
                """.formatted(patientId, doctorId))
            .post("/v1/exams")
            .jsonPath()
            .getString("id");

    given()
        .contentType(ContentType.JSON)
        .body("""
            {"result": "Hemoglobin 14.2 g/dL"}
            """)
        .when()
        .post("/v1/exams/{id}/result", examId)
        .then()
        .statusCode(200)
        .body("result", is("Hemoglobin 14.2 g/dL"))
        .body("resultRecordedAt", notNullValue());
  }
}
