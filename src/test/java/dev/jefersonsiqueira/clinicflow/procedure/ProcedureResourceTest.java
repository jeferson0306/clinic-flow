package dev.jefersonsiqueira.clinicflow.procedure;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProcedureResourceTest {

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
}
