package dev.jefersonsiqueira.clinicflow.patient;

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

@Path("/v1/patients")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PatientResource {

  @Inject PatientService service;

  @POST
  @Operation(summary = "Register a patient")
  @APIResponse(responseCode = "201", description = "Patient registered")
  @APIResponse(responseCode = "422", description = "A document failed validation")
  @APIResponse(responseCode = "409", description = "A patient with this CPF already exists")
  public Response register(@Valid CreatePatientRequest request) {
    Patient patient = service.register(request);
    return Response.created(URI.create("/v1/patients/" + patient.id))
        .entity(PatientResponse.from(patient))
        .build();
  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Fetch a patient by id")
  @APIResponse(responseCode = "200", description = "Patient found")
  @APIResponse(responseCode = "404", description = "No patient with this id")
  public PatientResponse findById(@PathParam("id") UUID id) {
    return PatientResponse.from(service.findById(id));
  }
}
