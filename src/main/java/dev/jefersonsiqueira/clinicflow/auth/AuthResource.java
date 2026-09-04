package dev.jefersonsiqueira.clinicflow.auth;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Two seeded demo accounts exist from V6's migration — {@code admin}/{@code
 * admin123} and {@code doctor}/{@code doctor123} — a public sandbox's
 * "pre-seeded demo accounts" now means logging in with these rather than
 * writing without logging in at all. Real credentials, real bcrypt, real
 * JWTs; the passwords are simply published, on purpose, the same as any
 * other public demo login.
 */
@Path("/v1/auth")
@Tag(name = "Auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RunOnVirtualThread
public class AuthResource {

  @Inject AuthService service;

  @POST
  @Path("/login")
  @Operation(summary = "Exchange a username and password for a JWT")
  @RequestBody(
      content =
          @Content(
              examples = {
                @ExampleObject(name = "admin", value = """
                    {"username": "admin", "password": "admin123"}"""),
                @ExampleObject(name = "doctor", value = """
                    {"username": "doctor", "password": "doctor123"}""")
              }))
  @APIResponse(
      responseCode = "200",
      description = "A bearer token, valid for 8 hours. Send it as `Authorization: Bearer <token>`.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "200",
                      value =
                          """
                          {
                            "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "expiresInSeconds": 28800,
                            "role": "ADMIN"
                          }""")))
  @APIResponse(
      responseCode = "401",
      description = "Wrong username or password — deliberately indistinguishable from each other.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "401",
                      value = """
                          {"field": null, "message": "Invalid username or password", "category": "UNAUTHORIZED"}""")))
  public LoginResponse login(@Valid LoginRequest request) {
    return service.login(request);
  }
}
