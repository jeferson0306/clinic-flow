package dev.jefersonsiqueira.clinicflow.validation.viacep;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * ViaCEP: free, public, no key, no auth — Brazil's standard postcode-to-address
 * lookup. Used to autofill a patient's street, district, city and state from
 * the postcode they typed, the "consulta em APIs públicas brasileiras" this
 * module exists for.
 *
 * Deliberately not on the registration path's critical path: see
 * {@link AddressLookupService} for why a lookup failure never blocks saving a
 * patient.
 */
@RegisterRestClient(configKey = "viacep")
public interface ViaCepClient {

  @GET
  @Path("/{postcode}/json/")
  ViaCepResponse lookup(@PathParam("postcode") String postcode);
}
