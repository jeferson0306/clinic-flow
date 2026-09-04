package dev.jefersonsiqueira.clinicflow.doctor;

import io.smallrye.common.annotation.RunOnVirtualThread;
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

@Path("/v1/doctors")
@Tag(name = "Doctors")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
// Every endpoint here blocks on JPA and, some of them, on brdoc over HTTP.
// @RunOnVirtualThread means that blocking costs a virtual thread parked by the
// JVM, not one of the small number of platform threads Quarkus's event loop
// runs on — the same throughput a fully reactive rewrite would buy, without one.
@RunOnVirtualThread
public class DoctorResource {

  @Inject DoctorService service;

  @POST
  @RolesAllowed("ADMIN")
  @Operation(
      summary = "Register a doctor",
      description =
          """
          CPF and email are validated and normalized through brdoc, same as a patient's. \
          The licence number (CRM) has no public check-digit algorithm to validate \
          against — only its presence and uniqueness are enforced, upper-cased and \
          trimmed on the way in.""")
  @RequestBody(
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "request",
                      value =
                          """
                          {
                            "fullName": "Dr. Marcos Lima",
                            "cpf": "529.982.247-25",
                            "email": "marcos@example.com",
                            "specialty": "Cardiology",
                            "licenseNumber": "12345-sp"
                          }""")))
  @APIResponse(
      responseCode = "201",
      description = "Doctor registered — licenseNumber comes back upper-cased.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "created",
                      value =
                          """
                          {
                            "id": "6cab716f-248f-43e7-b623-910349045d8e",
                            "fullName": "Dr. Marcos Lima",
                            "maskedCpf": "*********25",
                            "email": "marcos@example.com",
                            "specialty": "Cardiology",
                            "licenseNumber": "12345-SP",
                            "createdAt": "2026-09-04T17:22:22.948127Z"
                          }""")))
  @APIResponse(
      responseCode = "400",
      description = "The request body itself is malformed — see ProcedureResource's note.")
  @APIResponse(
      responseCode = "422",
      description = "brdoc rejected the CPF or the email. Shown here are the two fields that vary by situation; every error additionally carries category, traceId, timestamp and path — see PatientResource's 404 example for the complete shape.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "422",
                      value = """
                          {"field": "cpf", "message": "Invalid CPF format"}""")))
  @APIResponse(
      responseCode = "409",
      description = "A doctor with this CPF or this licence number is already registered. Shown here are the two fields that vary by situation; every error additionally carries category, traceId, timestamp and path — see PatientResource's 404 example for the complete shape.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "409",
                      value =
                          """
                          {"field": "licence number", "message": "A doctor with this licence number is already registered"}""")))
  public Response register(@Valid CreateDoctorRequest request) {
    Doctor doctor = service.register(request);
    return Response.created(URI.create("/v1/doctors/" + doctor.id))
        .entity(DoctorResponse.from(doctor))
        .build();
  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Fetch a doctor by id")
  @APIResponse(
      responseCode = "200",
      description = "Doctor found",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "success",
                      value =
                          """
                          {
                            "id": "6cab716f-248f-43e7-b623-910349045d8e",
                            "fullName": "Dr. Marcos Lima",
                            "maskedCpf": "*********25",
                            "email": "marcos@example.com",
                            "specialty": "Cardiology",
                            "licenseNumber": "12345-SP",
                            "createdAt": "2026-09-04T17:22:22.948127Z"
                          }""")))
  @APIResponse(responseCode = "404", description = "No doctor with this id")
  public DoctorResponse findById(@PathParam("id") UUID id) {
    return DoctorResponse.from(service.findById(id));
  }
}
