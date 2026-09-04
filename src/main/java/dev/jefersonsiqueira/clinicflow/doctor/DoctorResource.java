package dev.jefersonsiqueira.clinicflow.doctor;

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

@Path("/v1/doctors")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DoctorResource {

  @Inject DoctorService service;

  @POST
  @Operation(summary = "Register a doctor")
  @APIResponse(responseCode = "201", description = "Doctor registered")
  @APIResponse(responseCode = "422", description = "A document failed validation")
  @APIResponse(responseCode = "409", description = "A doctor with this CPF or licence number already exists")
  public Response register(@Valid CreateDoctorRequest request) {
    Doctor doctor = service.register(request);
    return Response.created(URI.create("/v1/doctors/" + doctor.id))
        .entity(DoctorResponse.from(doctor))
        .build();
  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Fetch a doctor by id")
  @APIResponse(responseCode = "200", description = "Doctor found")
  @APIResponse(responseCode = "404", description = "No doctor with this id")
  public DoctorResponse findById(@PathParam("id") UUID id) {
    return DoctorResponse.from(service.findById(id));
  }
}
