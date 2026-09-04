package dev.jefersonsiqueira.clinicflow.patient;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import dev.jefersonsiqueira.clinicflow.validation.brdoc.BrdocClient;
import dev.jefersonsiqueira.clinicflow.validation.brdoc.BrdocValidationResponse;
import dev.jefersonsiqueira.clinicflow.validation.viacep.ViaCepClient;
import dev.jefersonsiqueira.clinicflow.validation.viacep.ViaCepResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * brdoc and ViaCEP are mocked here, not called for real. A CI run should never
 * depend on a third-party free service being awake — Render's free tier
 * sleeps after 15 minutes idle — and the contract with brdoc is already
 * covered by its own test suite; this only needs to prove that a rejection
 * from it is handled correctly.
 */
@QuarkusTest
// Every write endpoint under test here now requires a role — see the
// RolesAllowed added alongside this. A blanket ADMIN+DOCTOR grant, not a
// real login, because these tests exist to verify business logic, not the
// RBAC boundary itself; AuthResourceIT and AuthorizationIT own that.
@TestSecurity(user = "test-user", roles = {"ADMIN", "DOCTOR"})
class PatientResourceIT {

  @InjectMock @RestClient BrdocClient brdoc;
  @InjectMock @RestClient ViaCepClient viaCep;

  @BeforeEach
  void validDocumentsByDefault() {
    // Echoes the real brdoc contract closely enough for these tests: strip
    // punctuation, report valid. Fixed elsewhere in this file only when a test
    // needs the *rejection* path — a stub that always returned the same
    // normalized CPF made every registered patient collide on the unique
    // constraint, which is the bug this shape avoids.
    Function<String, String> digitsOnly = raw -> raw.replaceAll("\\D", "");
    when(brdoc.validateCpf(anyString()))
        .thenAnswer(inv -> ok(new BrdocValidationResponse(
            true, digitsOnly.apply(inv.getArgument(0)), "Valid CPF format", null)));
    when(brdoc.validateEmail(anyString()))
        .thenAnswer(inv -> ok(new BrdocValidationResponse(true, inv.getArgument(0), "Valid email", null)));
    when(brdoc.validateCep(anyString()))
        .thenAnswer(inv -> ok(new BrdocValidationResponse(
            true, digitsOnly.apply(inv.getArgument(0)), "Valid postcode format", null)));
    when(viaCep.lookup(anyString()))
        .thenReturn(
            new ViaCepResponse("01310200", "Avenida Paulista", "Bela Vista", "São Paulo", "SP", false));
  }

  @Test
  void registersAPatientAndMasksTheCpfInTheResponse() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"fullName":"Ana Souza","cpf":"529.982.247-25","email":"ana@example.com","postcode":"01310-200"}
            """)
        .when()
        .post("/v1/patients")
        .then()
        .statusCode(201)
        .body("maskedCpf", endsWith("25"))
        .body("maskedCpf", is("*********25"))
        .body("address.city", is("São Paulo"));
  }

  private static Response ok(BrdocValidationResponse body) {
    return Response.ok(body).build();
  }

  private static Response unprocessable(BrdocValidationResponse body) {
    return Response.status(422).entity(body).build();
  }

  @Test
  void rejectsARegistrationBrdocRejects() {
    when(brdoc.validateCpf(anyString()))
        .thenReturn(unprocessable(
            new BrdocValidationResponse(false, "", "Invalid CPF check digits", "VALIDATION_FAILED")));

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"fullName":"Ana Souza","cpf":"111.111.111-11","email":"ana@example.com","postcode":"01310-200"}
            """)
        .when()
        .post("/v1/patients")
        .then()
        .statusCode(422)
        .body("field", is("cpf"));
  }

  @Test
  void savesAPatientEvenWhenViaCepIsDown() {
    when(viaCep.lookup(anyString())).thenThrow(new RuntimeException("connection refused"));

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"fullName":"Bruno Lima","cpf":"111.444.777-35","email":"bruno@example.com","postcode":"01310-200"}
            """)
        .when()
        .post("/v1/patients")
        .then()
        .statusCode(201)
        .body("address.city", is((Object) null));
  }
}
