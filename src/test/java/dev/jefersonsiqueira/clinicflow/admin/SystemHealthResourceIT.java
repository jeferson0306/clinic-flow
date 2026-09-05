package dev.jefersonsiqueira.clinicflow.admin;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(user = "an-admin", roles = "ADMIN")
class SystemHealthResourceIT {

  @Test
  void recordsAndReportsARejectedRequest() {
    // Any 4xx recorded by GlobalExceptionMapper should show up here — a
    // non-positive duration is the cheapest one to trigger deterministically.
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"name":"Health check probe","durationMinutes":0,"priceCents":1000}
            """)
        .when()
        .post("/v1/procedures")
        .then()
        .statusCode(422);

    given()
        .when()
        .get("/v1/admin/recent-errors")
        .then()
        .statusCode(200)
        .body("size()", greaterThan(0))
        .body("[0].status", equalTo(422))
        // The runtime type RESTEasy Reactive actually throws for a @Valid body
        // failure — a subclass of ConstraintViolationException, not that class
        // itself (see ConstraintViolationMapper's javadoc).
        .body("[0].exceptionType", equalTo("ResteasyReactiveViolationException"));
  }
}
