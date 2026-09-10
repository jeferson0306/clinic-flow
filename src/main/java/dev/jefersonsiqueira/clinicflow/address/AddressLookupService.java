package dev.jefersonsiqueira.clinicflow.address;

import dev.jefersonsiqueira.clinicflow.validation.brdoc.DocumentValidator;
import dev.jefersonsiqueira.clinicflow.validation.viacep.ViaCepClient;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Builds an {@link Address} from what the caller actually sent, validating
 * only the postcode's format through brdoc — the same as every other
 * document here.
 *
 * Street, district, city and state used to come exclusively from ViaCEP,
 * which meant a slow, down, or simply-doesn't-have-this-postcode response
 * left the whole address null. They are caller-supplied fields now (see
 * {@code CreatePatientRequest}/{@code UpdatePatientRequest}): the caller's
 * own form pre-fills them from ViaCEP as a courtesy, the person filling it
 * in can always correct or complete them by hand, and either way a real
 * value reaches here before the record is saved. ViaCEP still gets called
 * once, best-effort, purely to fill in {@code ibgeCode} — the one field
 * nobody can type in by hand and the caller was never asked to supply.
 */
@ApplicationScoped
public class AddressLookupService {

  @Inject DocumentValidator documentValidator;
  @Inject @RestClient ViaCepClient viaCep;

  public Address resolve(
      String rawPostcode, String street, String district, String city, String state) {
    String postcode = documentValidator.cep(rawPostcode);

    Address address = new Address();
    address.postcode = postcode;
    address.street = street;
    address.district = district;
    address.city = city;
    address.state = state == null ? null : state.toUpperCase();

    try {
      var found = viaCep.lookup(postcode);
      if (!found.notFound()) {
        address.ibgeCode = found.ibgeCode();
      }
    } catch (RuntimeException e) {
      Log.warnf("ViaCEP lookup failed for a postcode, continuing without ibgeCode: %s", e.getMessage());
    }
    return address;
  }
}
