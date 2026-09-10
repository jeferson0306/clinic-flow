package dev.jefersonsiqueira.clinicflow.auth;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Have I Been Pwned's range API — free, no key, no auth. K-anonymity: only
 * the first 5 hex characters of the password's SHA-1 hash are ever sent,
 * never the password or the full hash, so this service (and anyone
 * intercepting the request) learns nothing about which specific password
 * was checked among the thousands sharing that prefix. Response body is a
 * plain-text list of {@code SUFFIX:COUNT} lines, one per known-breached
 * password sharing the prefix — see {@link PasswordPolicy} for how that's
 * matched against.
 */
@RegisterRestClient(configKey = "hibp")
public interface PwnedPasswordChecker {

  @GET
  @Path("/range/{prefix}")
  @Produces(MediaType.TEXT_PLAIN)
  String range(@PathParam("prefix") String prefix);
}
