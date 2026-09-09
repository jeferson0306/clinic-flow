package dev.jefersonsiqueira.clinicflow.me;

import dev.jefersonsiqueira.clinicflow.appointment.AppointmentRepository;
import dev.jefersonsiqueira.clinicflow.appointment.AppointmentResponse;
import dev.jefersonsiqueira.clinicflow.exam.ExamRepository;
import dev.jefersonsiqueira.clinicflow.exam.ExamResponse;
import dev.jefersonsiqueira.clinicflow.patient.PatientResponse;
import dev.jefersonsiqueira.clinicflow.patient.PatientService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * The patient portal's own three read endpoints — deliberately not just
 * {@code PatientResource}/{@code AppointmentResource}/{@code ExamResource}
 * with a role added. None of these take an {@code id}: every one is scoped
 * to the {@code patientId} claim embedded in the caller's own JWT at login
 * (see {@code AuthService.issueAccessToken}), which a PACIENTE token cannot
 * forge or override. That is what makes it structurally impossible for one
 * patient to request another's data by guessing or trying a different UUID
 * — there is no UUID parameter here for one to try.
 */
@Path("/v1/me")
@Tag(name = "Patient portal")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("PACIENTE")
@RunOnVirtualThread
public class MeResource {

  @Inject JsonWebToken jwt;
  @Inject PatientService patients;
  @Inject AppointmentRepository appointments;
  @Inject ExamRepository exams;

  @GET
  @Path("/patient")
  @Operation(summary = "The signed-in patient's own record")
  public PatientResponse myPatientRecord() {
    return PatientResponse.from(patients.findById(myPatientId()));
  }

  @GET
  @Path("/appointments")
  @Operation(summary = "The signed-in patient's own appointments, newest first")
  public List<AppointmentResponse> myAppointments() {
    return appointments.findByPatientId(myPatientId()).stream().map(AppointmentResponse::from).toList();
  }

  @GET
  @Path("/exams")
  @Operation(summary = "The signed-in patient's own exams, newest first")
  public List<ExamResponse> myExams() {
    return exams.findByPatientId(myPatientId()).stream().map(ExamResponse::from).toList();
  }

  private UUID myPatientId() {
    String raw = jwt.getClaim("patientId");
    // Every PACIENTE token carries this — AuthService only sets the claim
    // when the User row has a non-null patientId, and only a PACIENTE user
    // ever has one. Reaching this with no claim means that invariant broke
    // somewhere else, not a normal "not found" a caller can act on.
    if (raw == null) {
      throw new IllegalStateException("PACIENTE token missing its patientId claim");
    }
    return UUID.fromString(raw);
  }
}
