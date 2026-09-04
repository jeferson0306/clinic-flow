package dev.jefersonsiqueira.clinicflow.appointment;

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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/v1/appointments")
@Produces(MediaType.APPLICATION_JSON)
public class AppointmentResource {

  @Inject AppointmentService service;

  @POST
  // Only this method takes a body; @Consumes at class level would apply it to
  // cancel() too, which has none — a bodyless POST does not send a
  // Content-Type, and JAX-RS rejects that as 415 against a class-wide
  // @Consumes it can never satisfy.
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Schedule an appointment")
  @APIResponse(responseCode = "201", description = "Appointment scheduled")
  @APIResponse(responseCode = "404", description = "The patient, doctor or procedure does not exist")
  @APIResponse(responseCode = "409", description = "The doctor already has an appointment in that time range")
  public Response schedule(@Valid ScheduleAppointmentRequest request) {
    Appointment appointment = service.schedule(request);
    return Response.created(URI.create("/v1/appointments/" + appointment.id))
        .entity(AppointmentResponse.from(appointment))
        .build();
  }

  @POST
  @Path("/{id}/cancel")
  @Operation(summary = "Cancel an appointment, freeing the doctor's slot")
  @APIResponse(responseCode = "200", description = "Appointment cancelled")
  @APIResponse(responseCode = "404", description = "No appointment with this id")
  public AppointmentResponse cancel(@PathParam("id") UUID id) {
    return AppointmentResponse.from(service.cancel(id));
  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Fetch an appointment by id")
  @APIResponse(responseCode = "200", description = "Appointment found")
  @APIResponse(responseCode = "404", description = "No appointment with this id")
  public AppointmentResponse findById(@PathParam("id") UUID id) {
    return AppointmentResponse.from(service.findById(id));
  }
}
