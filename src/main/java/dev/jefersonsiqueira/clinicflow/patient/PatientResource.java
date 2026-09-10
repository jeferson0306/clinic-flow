package dev.jefersonsiqueira.clinicflow.patient;

import dev.jefersonsiqueira.clinicflow.ratelimit.ClientAddressResolver;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.jwt.JsonWebToken;

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
  @Inject JsonWebToken jwt;
  @Inject ClientAddressResolver addressResolver;
  @Context SecurityContext securityContext;
  @Context HttpHeaders headers;
  @Context HttpServerRequest vertxRequest;

  /** Same derivation AuditLogFilter uses — kept here too because the CPF-change audit entry has to be written inside PatientService's own transaction, not from the filter that runs after it. */
  private String callerIp() {
    String cfConnectingIp = headers == null ? null : headers.getHeaderString("CF-Connecting-IP");
    String remoteAddress = vertxRequest == null ? null : vertxRequest.remoteAddress().hostAddress();
    return addressResolver.resolve(cfConnectingIp, remoteAddress);
  }

  private String callerRole() {
    return jwt.getGroups() == null || jwt.getGroups().isEmpty() ? null : jwt.getGroups().iterator().next();
  }

  @POST
  @RolesAllowed({"ADMIN", "RECEPCAO"})
  @Operation(
      summary = "Register a patient",
      description =
          """
          CPF, email and phone are validated and normalized through brdoc \
          before anything is stored — the CPF in the response is the normalized value, \
          masked. street/city/state are required inputs, not ViaCEP-derived \
          ones — the postcode lookup only pre-fills them and best-effort adds \
          ibgeCode; a slow or down ViaCEP, or a postcode it has never heard \
          of, never leaves the address incomplete (see AddressLookupService). \
          phone and birthDate are required: a clinic cannot reschedule an \
          appointment or deliver an exam result without a working phone \
          number, and birthDate is what lets the guardian-for-a-minor check \
          run at all (see RequiresGuardianIfMinor).""")
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
                          "postcode": "01310-200",
                          "houseNumber": "123",
                          "street": "Avenida Paulista",
                          "district": "Bela Vista",
                          "city": "Sao Paulo",
                          "state": "SP"
                        }"""),
                @ExampleObject(
                    name = "minor",
                    summary = "Under 18 — guardian name, CPF, relationship and phone all required",
                    value =
                        """
                        {
                          "fullName": "Joaozinho Silva",
                          "cpf": "111.444.777-35",
                          "email": "guardian-managed@example.com",
                          "phone": "+55 61 99194-6758",
                          "birthDate": "2015-01-01",
                          "postcode": "70040-010",
                          "houseNumber": "45",
                          "street": "SQN 210",
                          "city": "Brasilia",
                          "state": "DF",
                          "guardianName": "Bruno Lima",
                          "guardianCpf": "701.919.410-05",
                          "guardianRelationship": "PAI",
                          "guardianPhone": "+55 61 99194-1234"
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
      responseCode = "422",
      description = "The request body itself is malformed (a missing or badly-shaped field) or brdoc rejected a document — `field` names which one either way. Shown here are the two fields that vary by situation; every error additionally carries category, traceId, timestamp and path — see PatientResource's 404 example for the complete shape.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "422",
                      value = """
                          {"field": "cpf", "message": "Invalid CPF format"}""")))
  @APIResponse(
      responseCode = "409",
      description = "A patient with this CPF is already registered. Shown here are the two fields that vary by situation; every error additionally carries category, traceId, timestamp and path — see PatientResource's 404 example for the complete shape.",
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

  // Unlike ProcedureResource's reads (a public price list), these carry real
  // PHI — full name, CPF, birth date, phone, address — so both list and
  // findById require a real session, not just PermitAll-by-omission.
  //
  // RECEPCAO gets PatientSummaryResponse, never PatientResponse: the
  // clinical/guardian fields are not merely hidden by a UI, they never
  // leave the server for that role. See PatientSummaryResponse's javadoc.
  @GET
  @RolesAllowed({"ADMIN", "DOCTOR", "RECEPCAO"})
  @Operation(
      summary = "List every patient",
      description =
          "Newest first. No pagination — see PatientService.listAll's javadoc. RECEPCAO "
              + "receives PatientSummaryResponse (registration fields only); ADMIN and DOCTOR "
              + "receive the full PatientResponse, clinical and guardian fields included.")
  @APIResponse(
      responseCode = "200",
      description = "Every registered patient, CPF masked. Shape depends on the caller's role — see above.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "success",
                      value =
                          """
                          [
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
                            }
                          ]""")))
  public List<?> listAll() {
    List<Patient> all = service.listAll();
    return hasFullAccess()
        ? all.stream().map(PatientResponse::from).toList()
        : all.stream().map(PatientSummaryResponse::from).toList();
  }

  @GET
  @Path("/{id}")
  @RolesAllowed({"ADMIN", "DOCTOR", "RECEPCAO"})
  @Operation(
      summary = "Fetch a patient by id",
      description = "RECEPCAO receives PatientSummaryResponse; ADMIN and DOCTOR receive the full PatientResponse.")
  @APIResponse(
      responseCode = "200",
      description = "Patient found. Shape depends on the caller's role — see above.",
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
  @APIResponse(
      responseCode = "404",
      description =
          "No patient with this id. Every 404 across this API has this same shape — "
              + "field is always null here, only message, category, traceId, timestamp and path matter.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "404",
                      value =
                          """
                          {
                            "field": null,
                            "message": "Not found",
                            "category": "NOT_FOUND",
                            "traceId": "579976bbc70bd7971cd94fc6786cc105",
                            "timestamp": "2026-09-04T18:47:41.112383Z",
                            "path": "/v1/patients/00000000-0000-0000-0000-000000000000"
                          }""")))
  public Object findById(@PathParam("id") UUID id) {
    Patient patient = service.findById(id);
    return hasFullAccess() ? PatientResponse.from(patient) : PatientSummaryResponse.from(patient);
  }

  private boolean hasFullAccess() {
    return securityContext.isUserInRole("ADMIN") || securityContext.isUserInRole("DOCTOR");
  }

  @PUT
  @Path("/{id}")
  @RolesAllowed({"ADMIN", "RECEPCAO"})
  @Operation(
      summary = "Update a patient",
      description =
          "cpf is optional — blank keeps the CPF on file; a different, valid CPF is a genuine "
              + "correction (typo fixed at check-in, say) and requires cpfChangeReason, which is "
              + "then written to the audit log alongside the masked old and new values. Email and "
              + "phone are re-validated through brdoc; street/city/state are the caller's own "
              + "values, not re-resolved from postcode (see CreatePatientRequest's javadoc).")
  @APIResponse(responseCode = "200", description = "Patient updated")
  @APIResponse(responseCode = "404", description = "No patient with this id")
  @APIResponse(
      responseCode = "422",
      description =
          "brdoc rejected the email or phone, or cpf changed without a cpfChangeReason. Same shape as register's own 422.")
  @APIResponse(responseCode = "409", description = "The new CPF already belongs to another patient.")
  public PatientResponse update(@PathParam("id") UUID id, @Valid UpdatePatientRequest request) {
    return PatientResponse.from(service.update(id, request, jwt.getName(), callerRole(), callerIp()));
  }

  @DELETE
  @Path("/{id}")
  @RolesAllowed("ADMIN")
  @Operation(
      summary = "Delete a patient",
      description = "Rejected with 409 if the patient has any appointment or exam on record.")
  @APIResponse(responseCode = "204", description = "Patient deleted")
  @APIResponse(responseCode = "404", description = "No patient with this id")
  @APIResponse(
      responseCode = "409",
      description = "The patient has appointments or exams referencing it.")
  public Response delete(@PathParam("id") UUID id) {
    service.delete(id);
    return Response.noContent().build();
  }
}
