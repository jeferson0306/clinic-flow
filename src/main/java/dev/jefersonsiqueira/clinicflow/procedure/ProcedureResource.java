package dev.jefersonsiqueira.clinicflow.procedure;

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
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * {@code @Valid} on a request body is validated by RESTEasy Reactive itself,
 * before a resource method body ever runs — a failure there comes back as a
 * 400 in RESTEasy's own {@code {"title","status","violations"}} shape, not
 * this API's {@link dev.jefersonsiqueira.clinicflow.common.ApiError}. That is
 * deliberate, not an inconsistency to fix: 400 is the request itself being
 * malformed (missing a name, a non-positive duration); 422 is reserved for
 * {@code GlobalExceptionMapper}'s own cases, where the request is well-formed
 * but a business rule this API owns — a document brdoc rejected — says no.
 */
@Path("/v1/procedures")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
// Every endpoint here blocks on JPA and, some of them, on brdoc over HTTP.
// @RunOnVirtualThread means that blocking costs a virtual thread parked by the
// JVM, not one of the small number of platform threads Quarkus's event loop
// runs on — the same throughput a fully reactive rewrite would buy, without one.
@RunOnVirtualThread
public class ProcedureResource {

  @Inject ProcedureService service;

  @POST
  @Operation(summary = "Add a procedure to the catalogue")
  @APIResponse(responseCode = "201", description = "Procedure created")
  public Response create(@Valid CreateProcedureRequest request) {
    Procedure procedure = service.create(request);
    return Response.created(URI.create("/v1/procedures/" + procedure.id))
        .entity(ProcedureResponse.from(procedure))
        .build();
  }

  @GET
  @Operation(summary = "List the procedure catalogue")
  public List<ProcedureResponse> listAll() {
    return service.listAll().stream().map(ProcedureResponse::from).toList();
  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Fetch a procedure by id")
  @APIResponse(responseCode = "200", description = "Procedure found")
  @APIResponse(responseCode = "404", description = "No procedure with this id")
  public ProcedureResponse findById(@PathParam("id") UUID id) {
    return ProcedureResponse.from(service.findById(id));
  }
}
