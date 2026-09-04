package dev.jefersonsiqueira.clinicflow.procedure;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
// Every write endpoint under test here now requires a role — see the
// RolesAllowed added alongside this. A blanket ADMIN+DOCTOR grant, not a
// real login, because these tests exist to verify business logic, not the
// RBAC boundary itself; AuthResourceIT and AuthorizationIT own that.
@TestSecurity(user = "test-user", roles = {"ADMIN", "DOCTOR"})
class ProcedureResourceIT {

  @Test
  void createsAndListsAProcedure() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"name":"Consultation","durationMinutes":30,"priceCents":15000}
            """)
        .when()
        .post("/v1/procedures")
        .then()
        .statusCode(201)
        .body("name", is("Consultation"))
        .body("durationMinutes", is(30));

    given().when().get("/v1/procedures").then().statusCode(200).body("size()", greaterThanOrEqualTo(1));
  }

  @Test
  void rejectsANonPositiveDuration() {
    // 400, not this API's own 422: a @Valid failure on the request body is
    // caught and shaped by RESTEasy Reactive itself before GlobalExceptionMapper
    // ever sees it — see the note in ProcedureResource.
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"name":"Bad","durationMinutes":0,"priceCents":1000}
            """)
        .when()
        .post("/v1/procedures")
        .then()
        .statusCode(400);
  }

  @Test
  void updatesAProcedure() {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name":"Blood panel","durationMinutes":15,"priceCents":8000}
                """)
            .post("/v1/procedures")
            .jsonPath()
            .getString("id");

    given()
        .contentType(ContentType.JSON)
        .body("""
            {"name":"Complete blood panel","durationMinutes":20,"priceCents":9500}
            """)
        .when()
        .put("/v1/procedures/" + id)
        .then()
        .statusCode(200)
        .body("name", is("Complete blood panel"))
        .body("durationMinutes", is(20))
        .body("priceCents", is(9500));
  }

  @Test
  void deletesAProcedureWithNoAppointmentsAgainstIt() {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name":"Deletable","durationMinutes":10,"priceCents":5000}
                """)
            .post("/v1/procedures")
            .jsonPath()
            .getString("id");

    given().when().delete("/v1/procedures/" + id).then().statusCode(204);
    given().when().get("/v1/procedures/" + id).then().statusCode(404);
  }
}
