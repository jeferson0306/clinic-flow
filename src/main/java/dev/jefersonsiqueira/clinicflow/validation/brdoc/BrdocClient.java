package dev.jefersonsiqueira.clinicflow.validation.brdoc;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Talks to the deployed brdoc service instead of reimplementing CPF, email or
 * phone validation here. brdoc already carries the check-digit algorithms,
 * their tests against published vectors, and a production track record; a
 * second implementation in Java would only be a second place for the same
 * bug to be fixed once and re-introduced later.
 *
 * brdoc's endpoint takes the document type as the query parameter's own name
 * — {@code GET /v1/validate?cpf=...}, {@code ?email=...} — rather than as a
 * {@code key=}/{@code value=} pair, so this client has one method per document
 * type instead of a single generic one. Only the types this module actually
 * validates are declared; brdoc supports more (rg, name, telephone, and
 * others), added here if a module ends up needing them.
 *
 * The base URL comes from {@code quarkus.rest-client.brdoc.url} — see
 * application.properties — so a local run can point at a different instance
 * without a code change.
 *
 * Every method returns the raw {@link Response} rather than
 * {@link BrdocValidationResponse} directly. A rejected document is a 422 from
 * brdoc with a real, useful body — {@code is_valid: false}, a human-readable
 * message — not a failure, but the REST Client spec mandates a default
 * exception mapper that throws on any status >= 400 regardless of the
 * method's return type; returning {@code Response} alone does not opt out of
 * it (confirmed the hard way — see the discussion linked in
 * application.properties, next to the property that actually does).
 * {@code quarkus.rest-client."brdoc".disable-default-mapper=true} is what
 * turns it off. {@link DocumentValidator} reads the body itself, whatever the
 * status.
 */
@RegisterRestClient(configKey = "brdoc")
public interface BrdocClient {

  @GET
  @Path("/v1/validate")
  Response validateCpf(@QueryParam("cpf") String value);

  @GET
  @Path("/v1/validate")
  Response validateEmail(@QueryParam("email") String value);

  @GET
  @Path("/v1/validate")
  Response validateTelephone(@QueryParam("telephone") String value);

  @GET
  @Path("/v1/validate")
  Response validateCep(@QueryParam("cep") String value);
}
