package dev.jefersonsiqueira.clinicflow.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * The real credential path — bcrypt against the demo accounts V6/V11's
 * migrations seed, a real JWT issued and then actually accepted by a
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
            {"email": "admin@clinicflow.dev", "password": "admin123"}
            """)
        .when()
        .post("/v1/auth/login")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("role", is("ADMIN"));
  }

  @Test
  void loginIsCaseInsensitiveOnEmail() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"email": "Admin@ClinicFlow.Dev", "password": "admin123"}
            """)
        .when()
        .post("/v1/auth/login")
        .then()
        .statusCode(200)
        .body("role", is("ADMIN"));
  }

  @Test
  void rejectsAnEmailShapedLikeSomethingOtherThanAnEmail() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"email": "not-an-email", "password": "admin123"}
            """)
        .when()
        .post("/v1/auth/login")
        .then()
        // Bean Validation's own ConstraintViolationException, mapped to the
        // app's usual 422 VALIDATION shape — same path every other
        // @Valid-rejected field in this API takes, not a bespoke 400.
        .statusCode(422)
        .body("field", is("email"));
  }

  @Test
  void rejectsTheRightEmailWithTheWrongPassword() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"email": "admin@clinicflow.dev", "password": "not-the-password"}
            """)
        .when()
        .post("/v1/auth/login")
        .then()
        .statusCode(401)
        .body("message", is("Invalid email or password"));
  }

  @Test
  void rejectsAnEmailThatDoesNotExistWithTheSameMessage() {
    // Same message, same status, as a wrong password for a real user —
    // telling the two apart is an invitation to enumerate accounts.
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"email": "no-such-user@clinicflow.dev", "password": "anything"}
            """)
        .when()
        .post("/v1/auth/login")
        .then()
        .statusCode(401)
        .body("message", is("Invalid email or password"));
  }

  @Test
  void aTokenFromLoginIsActuallyAcceptedByAProtectedEndpoint() {
    String token =
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"email": "admin@clinicflow.dev", "password": "admin123"}
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

  @Test
  void changesPasswordAndCanLogInWithTheNewOne() {
    String token =
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"email": "doctor@clinicflow.dev", "password": "doctor123"}
                """)
            .post("/v1/auth/login")
            .jsonPath()
            .getString("token");

    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("""
            {"currentPassword": "doctor123", "newPassword": "NewPass!2026"}
            """)
        .when()
        .put("/v1/auth/password")
        .then()
        .statusCode(204);

    given()
        .contentType(ContentType.JSON)
        .body("""
            {"email": "doctor@clinicflow.dev", "password": "NewPass!2026"}
            """)
        .when()
        .post("/v1/auth/login")
        .then()
        .statusCode(200);

    // No cleanup: this class's Postgres container is scoped to this class
    // alone (Testcontainers, one per IT class) and no other test in this
    // class logs in as doctor@clinicflow.dev afterward — mutating this
    // account's password here does not touch the real seeded credentials,
    // which live only in the actual deployed database, never in a test
    // container.
  }

  @Test
  void loginReturnsAUsableRefreshToken() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"email": "admin@clinicflow.dev", "password": "admin123"}
            """)
        .when()
        .post("/v1/auth/login")
        .then()
        .statusCode(200)
        .body("refreshToken", notNullValue())
        .body("refreshExpiresInSeconds", is(604800)); // 7 days
  }

  @Test
  void refreshTradesAValidTokenForANewAccessAndRefreshPair() {
    String refreshToken =
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"email": "admin@clinicflow.dev", "password": "admin123"}
                """)
            .post("/v1/auth/login")
            .jsonPath()
            .getString("refreshToken");

    given()
        .contentType(ContentType.JSON)
        .body("{\"refreshToken\": \"" + refreshToken + "\"}")
        .when()
        .post("/v1/auth/refresh")
        .then()
        .statusCode(200)
        .body("token", notNullValue())
        .body("role", is("ADMIN"))
        .body("refreshToken", notNullValue());
  }

  @Test
  void aRotatedRefreshTokenCannotBeReused() {
    String refreshToken =
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"email": "admin@clinicflow.dev", "password": "admin123"}
                """)
            .post("/v1/auth/login")
            .jsonPath()
            .getString("refreshToken");

    given()
        .contentType(ContentType.JSON)
        .body("{\"refreshToken\": \"" + refreshToken + "\"}")
        .when()
        .post("/v1/auth/refresh")
        .then()
        .statusCode(200);

    // Same token again — already revoked by the rotation above.
    given()
        .contentType(ContentType.JSON)
        .body("{\"refreshToken\": \"" + refreshToken + "\"}")
        .when()
        .post("/v1/auth/refresh")
        .then()
        .statusCode(401);
  }

  @Test
  void refreshRejectsAnUnknownToken() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"refreshToken\": \"not-a-real-token\"}")
        .when()
        .post("/v1/auth/refresh")
        .then()
        .statusCode(401);
  }

  @Test
  void logoutRevokesTheRefreshTokenSoItCanNoLongerBeUsed() {
    String refreshToken =
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"email": "admin@clinicflow.dev", "password": "admin123"}
                """)
            .post("/v1/auth/login")
            .jsonPath()
            .getString("refreshToken");

    given()
        .contentType(ContentType.JSON)
        .body("{\"refreshToken\": \"" + refreshToken + "\"}")
        .when()
        .post("/v1/auth/logout")
        .then()
        .statusCode(204);

    given()
        .contentType(ContentType.JSON)
        .body("{\"refreshToken\": \"" + refreshToken + "\"}")
        .when()
        .post("/v1/auth/refresh")
        .then()
        .statusCode(401);
  }

  @Test
  void logoutIsIdempotent() {
    String refreshToken =
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"email": "admin@clinicflow.dev", "password": "admin123"}
                """)
            .post("/v1/auth/login")
            .jsonPath()
            .getString("refreshToken");

    String body = "{\"refreshToken\": \"" + refreshToken + "\"}";
    given().contentType(ContentType.JSON).body(body).when().post("/v1/auth/logout").then().statusCode(204);
    given().contentType(ContentType.JSON).body(body).when().post("/v1/auth/logout").then().statusCode(204);
  }

  @Test
  void rejectsAWeakNewPassword() {
    String token =
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"email": "admin@clinicflow.dev", "password": "admin123"}
                """)
            .post("/v1/auth/login")
            .jsonPath()
            .getString("token");

    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("""
            {"currentPassword": "admin123", "newPassword": "alllowercase"}
            """)
        .when()
        .put("/v1/auth/password")
        .then()
        .statusCode(422)
        .body("field", is("newPassword"));
  }
}
