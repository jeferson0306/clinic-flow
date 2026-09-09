package dev.jefersonsiqueira.clinicflow.auth;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Two seeded demo accounts exist from V6/V11's migrations — {@code
 * admin@clinicflow.dev}/{@code admin123} and {@code
 * doctor@clinicflow.dev}/{@code doctor123} — a public sandbox's "pre-seeded
 * demo accounts" now means logging in with these rather than writing without
 * logging in at all. Real credentials, real bcrypt, real JWTs; the passwords
 * are simply published, on purpose, the same as any other public demo login.
 */
@Path("/v1/auth")
@Tag(name = "Auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RunOnVirtualThread
public class AuthResource {

  @Inject AuthService service;
  @Inject JsonWebToken jwt;

  @POST
  @Path("/login")
  @Operation(summary = "Exchange an email and password for a JWT")
  @RequestBody(
      content =
          @Content(
              examples = {
                @ExampleObject(
                    name = "admin",
                    value = """
                    {"email": "admin@clinicflow.dev", "password": "admin123"}"""),
                @ExampleObject(
                    name = "doctor",
                    value = """
                    {"email": "doctor@clinicflow.dev", "password": "doctor123"}""")
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
      description = "Wrong email or password — deliberately indistinguishable from each other.",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "401",
                      value = """
                          {"field": null, "message": "Invalid email or password", "category": "UNAUTHORIZED"}""")))
  public LoginResponse login(@Valid LoginRequest request) {
    return service.login(request);
  }

  @POST
  @Path("/refresh")
  @PermitAll
  @Operation(
      summary = "Trade a still-valid refresh token for a new access token",
      description =
          """
          Rotates on every call: the refresh token sent here is revoked and a new one is \
          returned alongside the new access token, the same shape login's own 200 has. \
          A refresh token that is unknown, expired or already revoked (including a reused \
          one — rotation means it was only ever valid once) is rejected the same way wrong \
          credentials are.""")
  @APIResponse(responseCode = "200", description = "A new access+refresh token pair")
  @APIResponse(responseCode = "401", description = "The refresh token is invalid, expired or already used")
  public LoginResponse refresh(@Valid RefreshRequest request) {
    return service.refresh(request.refreshToken());
  }

  @POST
  @Path("/logout")
  @PermitAll
  @Operation(summary = "Revoke a refresh token", description = "Ends that session — the access token already issued still works until it expires on its own.")
  @APIResponse(responseCode = "204", description = "Revoked (or already was — logout is idempotent)")
  public Response logout(@Valid RefreshRequest request) {
    service.logout(request.refreshToken());
    return Response.noContent().build();
  }

  @PUT
  @Path("/password")
  @RolesAllowed({"ADMIN", "DOCTOR"})
  @Operation(summary = "Change the signed-in user's own password")
  @APIResponse(responseCode = "204", description = "Password changed")
  @APIResponse(responseCode = "401", description = "Current password was wrong")
  @APIResponse(
      responseCode = "422",
      description = "New password does not meet the strength policy (see PasswordPolicy)")
  public Response changePassword(@Valid ChangePasswordRequest request) {
    // jwt.getName() returns the `upn` claim — the same user.email AuthService
    // put there at login, never a value taken from the request body, so a
    // caller cannot change anyone's password but their own.
    service.changePassword(jwt.getName(), request);
    return Response.noContent().build();
  }
}
