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
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * The query side of scheduling: what a doctor has free on a given day, for a
 * given procedure's duration. {@code AppointmentResource.schedule} does not
 * need this — it is protected by the database's own exclusion constraint
 * regardless — but a calendar view has nothing to show without it.
 */
@Path("/v1/doctors/{doctorId}/availability")
@Tag(name = "Calendar")
@Produces(MediaType.APPLICATION_JSON)
@RunOnVirtualThread
public class CalendarResource {

  @Inject AvailabilityService service;

  @GET
  @Operation(
      summary = "List a doctor's free slots on a given day, for a given procedure",
      description =
          """
          The working window is fixed (clinic.calendar.start/end/zone-id, defaulting to \
          08:00-18:00 America/Sao_Paulo), the same for every doctor. Slots are sized to \
          the given procedure's durationMinutes and never overlap a SCHEDULED \
          appointment for that doctor.""")
  @APIResponse(
      responseCode = "200",
      description = "Free slots for that day, in order.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "response200",
                      value =
                          """
                          {
                            "doctorId": "6cab716f-248f-43e7-b623-910349045d8e",
                            "procedureId": "f896f53f-3ae0-4d71-b08e-4dc247f2fa98",
                            "freeSlots": [
                              {"startsAt": "2026-09-10T11:00:00Z", "endsAt": "2026-09-10T11:30:00Z"},
                              {"startsAt": "2026-09-10T11:30:00Z", "endsAt": "2026-09-10T12:00:00Z"}
                            ]
                          }""")))
  @APIResponse(responseCode = "404", description = "The doctor or the procedure does not exist")
  public AvailabilityResponse availability(
      @PathParam("doctorId") UUID doctorId,
      @QueryParam("date") LocalDate date,
      @QueryParam("procedureId") UUID procedureId) {
    return new AvailabilityResponse(doctorId, procedureId, service.freeSlotsFor(doctorId, date, procedureId));
  }
}
