package dev.jefersonsiqueira.clinicflow.exam;

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

@Path("/v1/exams")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExamResource {

  @Inject ExamService service;

  @POST
  @Operation(summary = "Request an exam for a patient")
  @APIResponse(responseCode = "201", description = "Exam requested")
  @APIResponse(responseCode = "404", description = "The patient or the requesting doctor does not exist")
  public Response request(@Valid RequestExamRequest request) {
    Exam exam = service.request(request);
    return Response.created(URI.create("/v1/exams/" + exam.id))
        .entity(ExamResponse.from(exam))
        .build();
  }

  @POST
  @Path("/{id}/result")
  @Operation(summary = "Record an exam's result")
  @APIResponse(responseCode = "200", description = "Result recorded")
  @APIResponse(responseCode = "404", description = "No exam with this id")
  public ExamResponse recordResult(@PathParam("id") UUID id, @Valid RecordExamResultRequest request) {
    return ExamResponse.from(service.recordResult(id, request));
  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Fetch an exam by id")
  @APIResponse(responseCode = "200", description = "Exam found")
  @APIResponse(responseCode = "404", description = "No exam with this id")
  public ExamResponse findById(@PathParam("id") UUID id) {
    return ExamResponse.from(service.findById(id));
  }
}
