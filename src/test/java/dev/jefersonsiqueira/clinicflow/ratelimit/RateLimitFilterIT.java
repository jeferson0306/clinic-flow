package dev.jefersonsiqueira.clinicflow.ratelimit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Runs against its own, separately-booted application context — see
 * {@link LowLimit} — so this is the one place in the suite the rate limit is
 * actually tight enough to exercise. Every other {@code *IT} class shares
 * one context (and therefore one client address) for the whole
 * {@code mvn verify} run, which is why {@code %test} keeps this filter's
 * limit effectively unlimited everywhere else — see application.properties.
 */
@QuarkusTest
@TestProfile(RateLimitFilterIT.LowLimit.class)
class RateLimitFilterIT {

  public static class LowLimit implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("clinic.rate-limit.requests-per-second", "0", "clinic.rate-limit.burst", "2");
    }
  }

  @Test
  void allowsExactlyTheBurstThenThrottles() {
    given().when().get("/v1/procedures").then().statusCode(200);
    given().when().get("/v1/procedures").then().statusCode(200);

    given()
        .when()
        .get("/v1/procedures")
        .then()
        .statusCode(429)
        .header("Retry-After", is("1"))
        .body("category", is("RATE_LIMITED"));
  }

  @Test
  void neverThrottlesTheHealthCheck() {
    // Drains the burst first, deliberately, rather than relying on JUnit's
    // undefined method order to have already done it via the test above.
    given().when().get("/v1/procedures");
    given().when().get("/v1/procedures");
    given().when().get("/v1/procedures").then().statusCode(429); // confirms the budget really is spent

    given().when().get("/q/health").then().statusCode(200);
  }
}
