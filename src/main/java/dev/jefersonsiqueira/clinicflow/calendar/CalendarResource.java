package dev.jefersonsiqueira.clinicflow.calendar;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * The query side of scheduling: what a doctor has free on a given day, for a
 * given procedure's duration. {@code AppointmentResource.schedule} does not
 * need this — it is protected by the database's own exclusion constraint
 * regardless — but a calendar view has nothing to show without it.
 */
@Path("/v1/doctors/{doctorId}/availability")
@Produces(MediaType.APPLICATION_JSON)
@RunOnVirtualThread
public class CalendarResource {

  @Inject AvailabilityService service;

  @GET
  @Operation(summary = "List a doctor's free slots on a given day, for a given procedure")
  @APIResponse(responseCode = "200", description = "Free slots for that day")
  @APIResponse(responseCode = "404", description = "The doctor or the procedure does not exist")
  public AvailabilityResponse availability(
      @PathParam("doctorId") UUID doctorId,
      @QueryParam("date") LocalDate date,
      @QueryParam("procedureId") UUID procedureId) {
    return new AvailabilityResponse(doctorId, procedureId, service.freeSlotsFor(doctorId, date, procedureId));
  }
}
