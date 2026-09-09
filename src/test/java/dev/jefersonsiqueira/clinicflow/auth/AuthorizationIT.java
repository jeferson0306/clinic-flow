package dev.jefersonsiqueira.clinicflow.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * Deliberately not class-annotated with {@code @TestSecurity} — every other
 * {@code *IT} class in this suite is, which is exactly why the RBAC boundary
 * itself has to live here instead of there: {@code @TestSecurity} at the
 * class level intercepts every request in that class and injects a fake
 * principal regardless of what was actually sent, which makes "no token at
 * all" untestable inside one. This class tests the three cases that
 * actually differ: no credentials, the wrong role, and the right one.
 */
@QuarkusTest
class AuthorizationIT {

  private static final String PROCEDURE_BODY =
      """
      {"name": "Boundary check", "durationMinutes": 30, "priceCents": 1000}
      """;

  @Test
  void rejectsAWriteWithNoCredentialsAtAll() {
    given().contentType(ContentType.JSON).body(PROCEDURE_BODY).when().post("/v1/procedures").then().statusCode(401);
  }

  @Test
  @TestSecurity(user = "a-doctor", roles = "DOCTOR")
  void rejectsAnAdminOnlyWriteFromTheWrongRole() {
    // ProcedureResource.create is @RolesAllowed("ADMIN") — a real, authenticated
    // DOCTOR is still the wrong role for it.
    given()
        .contentType(ContentType.JSON)
        .body(PROCEDURE_BODY)
        .when()
        .post("/v1/procedures")
        .then()
        .statusCode(403);
  }

  @Test
  @TestSecurity(user = "an-admin", roles = "ADMIN")
  void allowsTheWriteFromTheRightRole() {
    given().contentType(ContentType.JSON).body(PROCEDURE_BODY).when().post("/v1/procedures").then().statusCode(201);
  }

  @Test
  void readsStayPublicWithNoCredentials() {
    given().when().get("/v1/procedures").then().statusCode(200);
  }

  // Patients, doctors, appointments and exams carry real PHI, unlike the
  // procedures price list above — their reads must require a real session,
  // not just fall through PermitAll-by-omission the way they used to.
  @Test
  void patientReadsRejectNoCredentials() {
    given().when().get("/v1/patients").then().statusCode(401);
    given().when().get("/v1/patients/00000000-0000-0000-0000-000000000000").then().statusCode(401);
  }

  @Test
  void doctorReadsRejectNoCredentials() {
    given().when().get("/v1/doctors").then().statusCode(401);
    given().when().get("/v1/doctors/00000000-0000-0000-0000-000000000000").then().statusCode(401);
  }

  @Test
  void appointmentReadsRejectNoCredentials() {
    given().when().get("/v1/appointments").then().statusCode(401);
    given().when().get("/v1/appointments/00000000-0000-0000-0000-000000000000").then().statusCode(401);
  }

  @Test
  void examReadsRejectNoCredentials() {
    given().when().get("/v1/exams").then().statusCode(401);
    given().when().get("/v1/exams/00000000-0000-0000-0000-000000000000").then().statusCode(401);
  }

  @Test
  @TestSecurity(user = "a-doctor", roles = "DOCTOR")
  void patientReadsAllowARealDoctorSession() {
    given().when().get("/v1/patients").then().statusCode(200);
  }

  @Test
  void adminSystemHealthRejectsNoCredentials() {
    given().when().get("/v1/admin/recent-errors").then().statusCode(401);
  }

  @Test
  @TestSecurity(user = "a-doctor", roles = "DOCTOR")
  void adminSystemHealthRejectsTheWrongRole() {
    // Recent errors can carry request paths and exception types for every
    // resource in this service — a DOCTOR has no more business seeing that
    // than they do writing a procedure.
    given().when().get("/v1/admin/recent-errors").then().statusCode(403);
  }

  @Test
  @TestSecurity(user = "an-admin", roles = "ADMIN")
  void adminSystemHealthAllowsTheRightRole() {
    given().when().get("/v1/admin/recent-errors").then().statusCode(200);
  }

  // RECEPCAO: front desk — patients and appointments, not doctors,
  // procedures or exams. Same boundary ExamResource already draws between
  // DOCTOR and ADMIN, just for a third role.
  @Test
  @TestSecurity(user = "front-desk", roles = "RECEPCAO")
  void receptionCanReadPatientsAndAppointments() {
    given().when().get("/v1/patients").then().statusCode(200);
    given().when().get("/v1/appointments").then().statusCode(200);
    given().when().get("/v1/doctors").then().statusCode(200);
  }

  @Test
  @TestSecurity(user = "front-desk", roles = "RECEPCAO")
  void receptionCannotWriteADoctorOrAnExam() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"fullName":"Someone","cpf":"52998224725","email":"x@example.com","specialty":"Cardiology","licenseNumber":"1-SP"}
            """)
        .when()
        .post("/v1/doctors")
        .then()
        .statusCode(403);

    given()
        .contentType(ContentType.JSON)
        .body("""
            {"patientId":"00000000-0000-0000-0000-000000000000",
             "requestedByDoctorId":"00000000-0000-0000-0000-000000000000","type":"Blood count"}
            """)
        .when()
        .post("/v1/exams")
        .then()
        .statusCode(403);
  }

  // PACIENTE: only ever /v1/me/* — every plain staff route rejects it, the
  // same way a wrong staff role does.
  @Test
  @TestSecurity(user = "a-patient", roles = "PACIENTE")
  void patientRoleCannotReachAnyStaffRoute() {
    given().when().get("/v1/patients").then().statusCode(403);
    given().when().get("/v1/appointments").then().statusCode(403);
    given().when().get("/v1/exams").then().statusCode(403);
    given().when().get("/v1/admin/recent-errors").then().statusCode(403);
  }

  @Test
  @TestSecurity(user = "a-patient", roles = "PACIENTE")
  void patientRoleCanBrowseTheDoctorDirectory() {
    // The one staff-shaped read a patient is allowed: browsing who the
    // clinic's doctors are — a name and specialty, not PHI — is what lets
    // the portal show "Dr. X, Cardiology" on the patient's own appointments
    // (see MeResource) without leaking anyone else's patient data.
    given().when().get("/v1/doctors").then().statusCode(200);
  }
}
