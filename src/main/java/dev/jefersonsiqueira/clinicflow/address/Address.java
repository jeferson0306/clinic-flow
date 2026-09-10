package dev.jefersonsiqueira.clinicflow.address;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embedded into whichever entity has an address — a patient today, a clinic
 * location later — rather than given its own table. An address has no
 * identity or lifecycle of its own here: it is never queried, listed or
 * referenced independently of its owner.
 */
@Embeddable
public class Address {

  @Column(name = "postcode", length = 8)
  public String postcode;

  @Column(name = "street")
  public String street;

  @Column(name = "district")
  public String district;

  @Column(name = "city")
  public String city;

  @Column(name = "state", length = 2)
  public String state;

  /** The IBGE municipality code — ViaCEP already returns it, this just stops discarding it. */
  @Column(name = "ibge_code", length = 7)
  public String ibgeCode;

  /**
   * Neither of these two is derivable from the postcode — ViaCEP resolves a
   * street, not which building on it, so both come from the caller, not
   * {@link AddressLookupService}. {@code houseNumber} is still required:
   * an address with a street but no number is not a complete one, even
   * though it looks complete once ViaCEP fills the rest in automatically.
   */
  @Column(name = "house_number")
  public String houseNumber;

  @Column(name = "complement")
  public String complement;

  public static Address unresolved(String postcode) {
    Address address = new Address();
    address.postcode = postcode;
    return address;
  }
}
