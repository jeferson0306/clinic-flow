package dev.jefersonsiqueira.clinicflow.exam;

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
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/v1/exams")
@Tag(name = "Exams")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
// Every endpoint here blocks on JPA and, some of them, on brdoc over HTTP.
// @RunOnVirtualThread means that blocking costs a virtual thread parked by the
// JVM, not one of the small number of platform threads Quarkus's event loop
// runs on — the same throughput a fully reactive rewrite would buy, without one.
@RunOnVirtualThread
public class ExamResource {

  @Inject ExamService service;

  @POST
  @RolesAllowed("DOCTOR")
  @Operation(
      summary = "Request an exam for a patient",
      description =
          "No result yet — that comes later, through a separate call, once the exam is actually done.")
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
                            "requestedByDoctorId": "6cab716f-248f-43e7-b623-910349045d8e",
                            "type": "Complete blood count"
                          }""")))
  @APIResponse(
      responseCode = "201",
      description = "Exam requested. result and resultRecordedAt are null until recorded.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "created",
                      value =
                          """
                          {
                            "id": "3e5f7a10-2b1c-4e6d-9a8f-1234567890ab",
                            "patientId": "ef10c843-3fa7-46f2-90ba-daebc8d3edc7",
                            "requestedByDoctorId": "6cab716f-248f-43e7-b623-910349045d8e",
                            "type": "Complete blood count",
                            "requestedAt": "2026-09-04T18:00:00Z",
                            "result": null,
                            "resultRecordedAt": null
                          }""")))
  @APIResponse(
      responseCode = "404",
      description = "patientId or requestedByDoctorId does not name a real patient or doctor.")
  public Response request(@Valid RequestExamRequest request) {
    Exam exam = service.request(request);
    return Response.created(URI.create("/v1/exams/" + exam.id))
        .entity(ExamResponse.from(exam))
        .build();
  }

  @POST
  @Path("/{id}/result")
  @RolesAllowed("DOCTOR")
  @Operation(summary = "Record an exam's result")
  @RequestBody(
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "request",
                      value =
                          """
                          {"result": "Hemoglobin 14.2 g/dL — within reference range"}""")))
  @APIResponse(
      responseCode = "200",
      description = "Result recorded",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "success",
                      value =
                          """
                          {
                            "id": "3e5f7a10-2b1c-4e6d-9a8f-1234567890ab",
                            "patientId": "ef10c843-3fa7-46f2-90ba-daebc8d3edc7",
                            "requestedByDoctorId": "6cab716f-248f-43e7-b623-910349045d8e",
                            "type": "Complete blood count",
                            "requestedAt": "2026-09-04T18:00:00Z",
                            "result": "Hemoglobin 14.2 g/dL — within reference range",
                            "resultRecordedAt": "2026-09-05T09:12:00Z"
                          }""")))
  @APIResponse(responseCode = "404", description = "No exam with this id")
  public ExamResponse recordResult(@PathParam("id") UUID id, @Valid RecordExamResultRequest request) {
    return ExamResponse.from(service.recordResult(id, request));
  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Fetch an exam by id")
  @APIResponse(
      responseCode = "200",
      description = "Exam found — same shape as the two endpoints above.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "success",
                      value =
                          """
                          {
                            "id": "3e5f7a10-2b1c-4e6d-9a8f-1234567890ab",
                            "patientId": "ef10c843-3fa7-46f2-90ba-daebc8d3edc7",
                            "requestedByDoctorId": "6cab716f-248f-43e7-b623-910349045d8e",
                            "type": "Complete blood count",
                            "requestedAt": "2026-09-04T18:00:00Z",
                            "result": "Hemoglobin 14.2 g/dL — within reference range",
                            "resultRecordedAt": "2026-09-05T09:12:00Z"
                          }""")))
  @APIResponse(responseCode = "404", description = "No exam with this id")
  public ExamResponse findById(@PathParam("id") UUID id) {
    return ExamResponse.from(service.findById(id));
  }

  @GET
  @Operation(summary = "List every exam", description = "Newest first (by requestedAt). No pagination yet.")
  @APIResponse(
      responseCode = "200",
      description = "Every exam, resulted or still pending.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "success",
                      value =
                          """
                          [
                            {
                              "id": "3e5f7a10-2b1c-4e6d-9a8f-1234567890ab",
                              "patientId": "ef10c843-3fa7-46f2-90ba-daebc8d3edc7",
                              "requestedByDoctorId": "6cab716f-248f-43e7-b623-910349045d8e",
                              "type": "Complete blood count",
                              "requestedAt": "2026-09-04T18:00:00Z",
                              "result": null,
                              "resultRecordedAt": null
                            }
                          ]""")))
  public List<ExamResponse> listAll() {
    return service.listAll().stream().map(ExamResponse::from).toList();
  }
}
