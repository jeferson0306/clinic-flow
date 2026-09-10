package dev.jefersonsiqueira.clinicflow.audit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import dev.jefersonsiqueira.clinicflow.validation.brdoc.BrdocClient;
import dev.jefersonsiqueira.clinicflow.validation.brdoc.BrdocValidationResponse;
import dev.jefersonsiqueira.clinicflow.validation.viacep.ViaCepClient;
import dev.jefersonsiqueira.clinicflow.validation.viacep.ViaCepResponse;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Confirms {@link AuditLogFilter} actually writes — every other IT in this
 * suite proves behavior through the HTTP API alone, but there is no
 * read-back endpoint for the audit trail yet (deliberately, see its own
 * javadoc), so this is the one place a direct query is the only way to
 * observe the effect at all.
 */
@QuarkusTest
@TestSecurity(user = "test-user", roles = {"ADMIN", "DOCTOR"})
class AuditLogFilterIT {

  @InjectMock @RestClient BrdocClient brdoc;
  @InjectMock @RestClient ViaCepClient viaCep;
  @Inject AuditLogRepository auditLog;

  @BeforeEach
  void validDocumentsByDefault() {
    when(brdoc.validateCpf(anyString()))
        .thenAnswer(inv -> Response.ok(new BrdocValidationResponse(
                true, ((String) inv.getArgument(0)).replaceAll("\\D", ""), "Valid CPF format", null))
            .build());
    when(brdoc.validateEmail(anyString()))
        .thenAnswer(inv -> Response.ok(new BrdocValidationResponse(true, inv.getArgument(0), "Valid email", null))
            .build());
    when(brdoc.validateCep(anyString()))
        .thenAnswer(inv -> Response.ok(new BrdocValidationResponse(
                true, ((String) inv.getArgument(0)).replaceAll("\\D", ""), "Valid postcode format", null))
            .build());
    when(viaCep.lookup(anyString()))
        .thenReturn(new ViaCepResponse("01310200", "Avenida Paulista", "Bela Vista", "São Paulo", "SP", "3550308", false));
  }

  @Test
  void readingAPatientListWritesAViewedEntry() {
    long before = QuarkusTransaction.requiringNew().call(auditLog::count);

    given().when().get("/v1/patients").then().statusCode(200);

    long after = QuarkusTransaction.requiringNew().call(auditLog::count);
    assertThat(after).isGreaterThan(before);

    AuditLogEntry last = QuarkusTransaction.requiringNew()
        .call(() -> auditLog.find("resourceType = ?1 order by occurredAt desc", "PATIENT").firstResult());
    assertThat(last).isNotNull();
    assertThat(last.action).isEqualTo(AuditAction.VIEWED);
    assertThat(last.actorEmail).isEqualTo("test-user");
  }

  @Test
  void creatingAPatientWritesACreatedEntry() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"fullName":"Auditada Silva","cpf":"123.456.789-09","email":"auditada@example.com","postcode":"01310-200","houseNumber":"123"}
            """)
        .when()
        .post("/v1/patients")
        .then()
        .statusCode(201);

    AuditLogEntry last = QuarkusTransaction.requiringNew()
        .call(() ->
            auditLog.find("resourceType = ?1 and action = ?2 order by occurredAt desc", "PATIENT", AuditAction.CREATED)
                .firstResult());
    assertThat(last).isNotNull();
  }
}
