package dev.jefersonsiqueira.clinicflow.address;

import dev.jefersonsiqueira.clinicflow.validation.brdoc.DocumentValidator;
import dev.jefersonsiqueira.clinicflow.validation.viacep.ViaCepClient;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Resolves a postcode to a street, district, city and state.
 *
 * The postcode's <em>format</em> is validated through brdoc, same as every
 * other document here — a malformed value is rejected before anything is
 * saved. The address <em>content</em> is a courtesy from ViaCEP on top of
 * that, and courtesies are not allowed to block the thing they were meant to
 * help with: if ViaCEP is slow, down, or the postcode is real but unlisted,
 * registration proceeds with the postcode alone rather than failing a form
 * over a free third-party API's uptime. Only the postcode's own format check
 * — the part this service actually owns — can reject the request.
 */
@ApplicationScoped
public class AddressLookupService {

  @Inject DocumentValidator documentValidator;
  @Inject @RestClient ViaCepClient viaCep;

  public Address resolve(String rawPostcode) {
    String postcode = documentValidator.cep(rawPostcode);

    try {
      var found = viaCep.lookup(postcode);
      if (found.notFound()) {
        return Address.unresolved(postcode);
      }
      Address address = new Address();
      address.postcode = postcode;
      address.street = found.street();
      address.district = found.district();
      address.city = found.city();
      address.state = found.state();
      return address;
    } catch (RuntimeException e) {
      Log.warnf("ViaCEP lookup failed for a postcode, continuing without it: %s", e.getMessage());
      return Address.unresolved(postcode);
    }
  }
}
