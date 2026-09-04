package dev.jefersonsiqueira.clinicflow.validation.brdoc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * The one place a module reaches for a brdoc check, so the pattern —
 * validate, and on failure raise with the field name and brdoc's own message —
 * is written once rather than copied into every resource that touches a CPF.
 *
 * Every method returns the <em>normalized</em> value on success. A patient's
 * CPF is stored as brdoc normalized it, not as the caller typed it — the
 * lesson from brdoc's own history: a mask formats, validation decides, and
 * storing the raw input is how "529.982.247-25garbage" once passed as valid
 * elsewhere.
 */
@ApplicationScoped
public class DocumentValidator {

  @Inject @RestClient BrdocClient brdoc;

  public String cpf(String rawValue) {
    return require("cpf", brdoc.validateCpf(rawValue));
  }

  public String email(String rawValue) {
    return require("email", brdoc.validateEmail(rawValue));
  }

  public String telephone(String rawValue) {
    return require("telephone", brdoc.validateTelephone(rawValue));
  }

  public String cep(String rawValue) {
    return require("postcode", brdoc.validateCep(rawValue));
  }

  // BrdocClient's methods return the raw Response — see its javadoc for why —
  // so reading the body is this method's job, whatever the status code.
  private String require(String field, Response httpResponse) {
    BrdocValidationResponse response = httpResponse.readEntity(BrdocValidationResponse.class);
    if (!response.valid()) {
      throw new DocumentValidationException(field, response.message());
    }
    return response.normalizedValue();
  }
}
