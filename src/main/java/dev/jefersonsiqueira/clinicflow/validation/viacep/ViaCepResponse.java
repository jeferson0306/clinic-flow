package dev.jefersonsiqueira.clinicflow.validation.viacep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ViaCEP's fields, in the Portuguese it publishes them in — this is the wire
 * format of a specific third-party API, not this codebase's own vocabulary,
 * so it stays as-is here and is translated at the boundary where it is used.
 * ViaCEP answers a nonexistent postcode with HTTP 200 and {@code erro: true}
 * rather than a 404, hence that field.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViaCepResponse(
    @JsonProperty("cep") String postcode,
    @JsonProperty("logradouro") String street,
    @JsonProperty("bairro") String district,
    @JsonProperty("localidade") String city,
    @JsonProperty("uf") String state,
    @JsonProperty("erro") boolean notFound) {}
