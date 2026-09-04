package dev.jefersonsiqueira.clinicflow.doctor;

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
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
// Every write endpoint under test here now requires a role — see the
// RolesAllowed added alongside this. A blanket ADMIN+DOCTOR grant, not a
// real login, because these tests exist to verify business logic, not the
// RBAC boundary itself; AuthResourceIT and AuthorizationIT own that.
@TestSecurity(user = "test-user", roles = {"ADMIN", "DOCTOR"})
class DoctorResourceIT {

  @InjectMock @RestClient BrdocClient brdoc;

  @BeforeEach
  void validDocumentsByDefault() {
    when(brdoc.validateCpf(anyString()))
        .thenAnswer(inv -> ok(new BrdocValidationResponse(
            true, ((String) inv.getArgument(0)).replaceAll("\\D", ""), "Valid CPF format", null)));
    when(brdoc.validateEmail(anyString()))
        .thenAnswer(inv -> ok(new BrdocValidationResponse(true, inv.getArgument(0), "Valid email", null)));
  }

  private static Response ok(BrdocValidationResponse body) {
    return Response.ok(body).build();
  }

  @Test
  void registersADoctorAndMasksTheCpfInTheResponse() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"fullName":"Dr. Rui Costa","cpf":"529.982.247-25","email":"rui@example.com","specialty":"Cardiology","licenseNumber":"12345-sp"}
            """)
        .when()
        .post("/v1/doctors")
        .then()
        .statusCode(201)
        .body("maskedCpf", is("*********25"))
        // Uppercased on the way in, so "sp" is stored and returned as "SP".
        .body("licenseNumber", is("12345-SP"));
  }

  @Test
  void rejectsASecondDoctorWithTheSameLicenceNumber() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"fullName":"Dr. Ana Melo","cpf":"111.444.777-35","email":"ana.melo@example.com","specialty":"Dermatology","licenseNumber":"99999-RJ"}
            """)
        .when()
        .post("/v1/doctors")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"fullName":"Dr. Bento Alves","cpf":"390.533.447-05","email":"bento@example.com","specialty":"Dermatology","licenseNumber":"99999-rj"}
            """)
        .when()
        .post("/v1/doctors")
        .then()
        .statusCode(409)
        .body("field", is("licence number"));
  }

  @Test
  void updatesADoctorWithoutTouchingTheCpf() {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {"fullName":"Dr. Elis Souza","cpf":"216.508.510-08","email":"elis@example.com","specialty":"Neurology","licenseNumber":"11111-SP"}
                """)
            .post("/v1/doctors")
            .jsonPath()
            .getString("id");

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"fullName":"Dr. Elis Souza Lima","email":"elis.lima@example.com","specialty":"Neurology","licenseNumber":"22222-sp"}
            """)
        .when()
        .put("/v1/doctors/" + id)
        .then()
        .statusCode(200)
        .body("fullName", is("Dr. Elis Souza Lima"))
        .body("licenseNumber", is("22222-SP"))
        .body("maskedCpf", is("*********08"));
  }

  @Test
  void rejectsAnUpdateThatCollidesWithAnotherDoctorsLicenceNumber() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"fullName":"Dr. First","cpf":"701.919.410-05","email":"first@example.com","specialty":"Dermatology","licenseNumber":"33333-RJ"}
            """)
        .post("/v1/doctors")
        .then()
        .statusCode(201);

    String secondId =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {"fullName":"Dr. Second","cpf":"999.888.777-00","email":"second@example.com","specialty":"Dermatology","licenseNumber":"44444-RJ"}
                """)
            .post("/v1/doctors")
            .jsonPath()
            .getString("id");

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"fullName":"Dr. Second","email":"second@example.com","specialty":"Dermatology","licenseNumber":"33333-rj"}
            """)
        .when()
        .put("/v1/doctors/" + secondId)
        .then()
        .statusCode(409)
        .body("field", is("licence number"));
  }

  @Test
  void deletesADoctorWithNoRecordsAgainstThem() {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {"fullName":"Dr. Deletable","cpf":"186.398.410-00","email":"deletable@example.com","specialty":"Pediatrics","licenseNumber":"55555-SP"}
                """)
            .post("/v1/doctors")
            .jsonPath()
            .getString("id");

    given().when().delete("/v1/doctors/" + id).then().statusCode(204);
    given().when().get("/v1/doctors/" + id).then().statusCode(404);
  }
}
