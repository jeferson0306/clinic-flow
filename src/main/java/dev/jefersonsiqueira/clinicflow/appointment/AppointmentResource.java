package dev.jefersonsiqueira.clinicflow.appointment;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/v1/appointments")
@Tag(name = "Appointments")
@Produces(MediaType.APPLICATION_JSON)
// Every endpoint here blocks on JPA and, some of them, on brdoc over HTTP.
// @RunOnVirtualThread means that blocking costs a virtual thread parked by the
// JVM, not one of the small number of platform threads Quarkus's event loop
// runs on — the same throughput a fully reactive rewrite would buy, without one.
@RunOnVirtualThread
public class AppointmentResource {

  @Inject AppointmentService service;

  @POST
  // Only this method takes a body; @Consumes at class level would apply it to
  // cancel() too, which has none — a bodyless POST does not send a
  // Content-Type, and JAX-RS rejects that as 415 against a class-wide
  // @Consumes it can never satisfy.
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({"ADMIN", "DOCTOR"})
  @Operation(
      summary = "Schedule an appointment",
      description =
          """
          endsAt is derived from the procedure's own durationMinutes — not sent by the \
          caller. A doctor cannot be double-booked; that guarantee is a Postgres \
          EXCLUDE constraint (see V5's migration), not application code — this API \
          surfaces the conflict as 409, but the guarantee itself holds even under \
          concurrent requests this API's own code never sees race.""")
  @RequestBody(
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "request",
                      value =
                          """
                          {
                            "patientId": "ef10c843-3fa7-46f2-90ba-daebc8d3edc7",
                            "doctorId": "6cab716f-248f-43e7-b623-910349045d8e",
                            "procedureId": "f896f53f-3ae0-4d71-b08e-4dc247f2fa98",
                            "startsAt": "2026-09-10T11:00:00Z"
                          }""")))
  @APIResponse(
      responseCode = "201",
      description = "Appointment scheduled",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "created",
                      value =
                          """
                          {
                            "id": "172ef31c-091d-4aac-9f57-2041f3a1d842",
                            "patientId": "ef10c843-3fa7-46f2-90ba-daebc8d3edc7",
                            "doctorId": "6cab716f-248f-43e7-b623-910349045d8e",
                            "procedureId": "f896f53f-3ae0-4d71-b08e-4dc247f2fa98",
                            "startsAt": "2026-09-10T11:00:00Z",
                            "endsAt": "2026-09-10T11:30:00Z",
                            "status": "SCHEDULED"
                          }""")))
  @APIResponse(responseCode = "404", description = "The patient, doctor or procedure does not exist")
  @APIResponse(
      responseCode = "409",
      description = "The doctor already has a SCHEDULED appointment overlapping this time range. Shown here are the two fields that vary by situation; every error additionally carries category, traceId, timestamp and path — see PatientResource's 404 example for the complete shape.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "409",
                      value =
                          """
                          {"field": "startsAt", "message": "This doctor already has an appointment scheduled in that time range"}""")))
  public Response schedule(@Valid ScheduleAppointmentRequest request) {
    Appointment appointment = service.schedule(request);
    return Response.created(URI.create("/v1/appointments/" + appointment.id))
        .entity(AppointmentResponse.from(appointment))
        .build();
  }

  @POST
  @Path("/{id}/cancel")
  @RolesAllowed({"ADMIN", "DOCTOR"})
  @Operation(
      summary = "Cancel an appointment, freeing the doctor's slot",
      description =
          "The exclusion constraint only applies to SCHEDULED rows — cancelling is what actually frees the slot for another booking.")
  @APIResponse(
      responseCode = "200",
      description = "Appointment cancelled",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "success",
                      value =
                          """
                          {
                            "id": "172ef31c-091d-4aac-9f57-2041f3a1d842",
                            "patientId": "ef10c843-3fa7-46f2-90ba-daebc8d3edc7",
                            "doctorId": "6cab716f-248f-43e7-b623-910349045d8e",
                            "procedureId": "f896f53f-3ae0-4d71-b08e-4dc247f2fa98",
                            "startsAt": "2026-09-10T11:00:00Z",
                            "endsAt": "2026-09-10T11:30:00Z",
                            "status": "CANCELLED"
                          }""")))
  @APIResponse(responseCode = "404", description = "No appointment with this id")
  public AppointmentResponse cancel(@PathParam("id") UUID id) {
    return AppointmentResponse.from(service.cancel(id));
  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Fetch an appointment by id")
  @APIResponse(
      responseCode = "200",
      description = "Appointment found — same shape as schedule's own 201.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "success",
                      value =
                          """
                          {
                            "id": "172ef31c-091d-4aac-9f57-2041f3a1d842",
                            "patientId": "ef10c843-3fa7-46f2-90ba-daebc8d3edc7",
                            "doctorId": "6cab716f-248f-43e7-b623-910349045d8e",
                            "procedureId": "f896f53f-3ae0-4d71-b08e-4dc247f2fa98",
                            "startsAt": "2026-09-10T11:00:00Z",
                            "endsAt": "2026-09-10T11:30:00Z",
                            "status": "SCHEDULED"
                          }""")))
  @APIResponse(responseCode = "404", description = "No appointment with this id")
  public AppointmentResponse findById(@PathParam("id") UUID id) {
    return AppointmentResponse.from(service.findById(id));
  }
}
