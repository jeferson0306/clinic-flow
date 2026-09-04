package dev.jefersonsiqueira.clinicflow.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * The real credential path — bcrypt against the demo accounts V6's
 * migration seeds, a real JWT issued and then actually accepted by a
 * protected endpoint. No {@code @TestSecurity} anywhere in this class: that
 * is for tests that only need *a* valid principal to exercise business
 * logic, and this class is the one place login itself is what is on trial.
 */
@QuarkusTest
class AuthResourceIT {

  @Test
  void logsInWithTheSeededAdminAccount() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"username": "admin", "password": "admin123"}
            """)
        .when()
        .post("/v1/auth/login")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("role", is("ADMIN"));
  }

  @Test
  void rejectsTheRightUsernameWithTheWrongPassword() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"username": "admin", "password": "not-the-password"}
            """)
        .when()
        .post("/v1/auth/login")
        .then()
        .statusCode(401)
        .body("message", is("Invalid username or password"));
  }

  @Test
  void rejectsAUsernameThatDoesNotExistWithTheSameMessage() {
    // Same message, same status, as a wrong password for a real user —
    // telling the two apart is an invitation to enumerate usernames.
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"username": "no-such-user", "password": "anything"}
            """)
        .when()
        .post("/v1/auth/login")
        .then()
        .statusCode(401)
        .body("message", is("Invalid username or password"));
  }

  @Test
  void aTokenFromLoginIsActuallyAcceptedByAProtectedEndpoint() {
    String token =
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "admin", "password": "admin123"}
                """)
            .post("/v1/auth/login")
            .jsonPath()
            .getString("token");

    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("""
            {"name": "End-to-end login check", "durationMinutes": 15, "priceCents": 500}
            """)
        .when()
        .post("/v1/procedures")
        .then()
        .statusCode(201);
  }
}
