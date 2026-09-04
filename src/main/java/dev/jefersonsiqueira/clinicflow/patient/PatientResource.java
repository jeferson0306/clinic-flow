package dev.jefersonsiqueira.clinicflow.patient;

import io.smallrye.common.annotation.RunOnVirtualThread;
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

@Path("/v1/patients")
@Tag(name = "Patients")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
// Every endpoint here blocks on JPA and, some of them, on brdoc over HTTP.
// @RunOnVirtualThread means that blocking costs a virtual thread parked by the
// JVM, not one of the small number of platform threads Quarkus's event loop
// runs on — the same throughput a fully reactive rewrite would buy, without one.
@RunOnVirtualThread
public class PatientResource {

  @Inject PatientService service;

  @POST
  @Operation(
      summary = "Register a patient",
      description =
          """
          CPF, email and, if given, phone are validated and normalized through brdoc \
          before anything is stored — the CPF in the response is the normalized value, \
          masked. The postcode is resolved to a street, district, city and state via \
          ViaCEP; that lookup is a courtesy and never blocks registration if ViaCEP is \
          slow or down (see AddressLookupService).""")
  @RequestBody(
      content =
          @Content(
              examples = {
                @ExampleObject(
                    name = "valid",
                    summary = "A registerable patient",
                    value =
                        """
                        {
                          "fullName": "Ana Souza",
                          "cpf": "529.982.247-25",
                          "email": "ana@example.com",
                          "phone": "+55 61 99194-6758",
                          "birthDate": "1990-05-10",
                          "postcode": "01310-200"
                        }"""),
                @ExampleObject(
                    name = "minimal",
                    summary = "Only what is required — phone and birth date are optional",
                    value =
                        """
                        {
                          "fullName": "Bruno Lima",
                          "cpf": "111.444.777-35",
                          "email": "bruno@example.com",
                          "postcode": "70040-010"
                        }""")
              }))
  @APIResponse(
      responseCode = "201",
      description = "Patient registered. The CPF comes back masked; the address, resolved by ViaCEP.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "created",
                      value =
                          """
                          {
                            "id": "ef10c843-3fa7-46f2-90ba-daebc8d3edc7",
                            "fullName": "Ana Souza",
                            "maskedCpf": "*********25",
                            "email": "ana@example.com",
                            "phone": "61991946758",
                            "birthDate": "1990-05-10",
                            "address": {
                              "postcode": "01310200",
                              "street": "Avenida Paulista",
                              "district": "Bela Vista",
                              "city": "São Paulo",
                              "state": "SP"
                            },
                            "createdAt": "2026-09-04T14:41:46.722547Z"
                          }""")))
  @APIResponse(
      responseCode = "400",
      description =
          "The request body itself is malformed — a missing field, not a business rule. "
              + "Shaped by RESTEasy Reactive, not this API's own error format; see ProcedureResource's note.")
  @APIResponse(
      responseCode = "422",
      description = "brdoc rejected a document. `field` names which one.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "422",
                      value = """
                          {"field": "cpf", "message": "Invalid CPF format"}""")))
  @APIResponse(
      responseCode = "409",
      description = "A patient with this CPF is already registered.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "409",
                      value =
                          """
                          {"field": "cpf", "message": "A patient with this CPF is already registered"}""")))
  public Response register(@Valid CreatePatientRequest request) {
    Patient patient = service.register(request);
    return Response.created(URI.create("/v1/patients/" + patient.id))
        .entity(PatientResponse.from(patient))
        .build();
  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Fetch a patient by id")
  @APIResponse(
      responseCode = "200",
      description = "Patient found",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "success",
                      value =
                          """
                          {
                            "id": "ef10c843-3fa7-46f2-90ba-daebc8d3edc7",
                            "fullName": "Ana Souza",
                            "maskedCpf": "*********25",
                            "email": "ana@example.com",
                            "phone": "61991946758",
                            "birthDate": "1990-05-10",
                            "address": {
                              "postcode": "01310200",
                              "street": "Avenida Paulista",
                              "district": "Bela Vista",
                              "city": "São Paulo",
                              "state": "SP"
                            },
                            "createdAt": "2026-09-04T14:41:46.722547Z"
                          }""")))
  @APIResponse(responseCode = "404", description = "No patient with this id")
  public PatientResponse findById(@PathParam("id") UUID id) {
    return PatientResponse.from(service.findById(id));
  }
}
