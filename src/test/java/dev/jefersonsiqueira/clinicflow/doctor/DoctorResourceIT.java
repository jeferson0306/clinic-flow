package dev.jefersonsiqueira.clinicflow.doctor;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import dev.jefersonsiqueira.clinicflow.validation.brdoc.BrdocClient;
import dev.jefersonsiqueira.clinicflow.validation.brdoc.BrdocValidationResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
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
}
