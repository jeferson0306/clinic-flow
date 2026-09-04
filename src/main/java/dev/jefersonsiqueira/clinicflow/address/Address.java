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

  public static Address unresolved(String postcode) {
    Address address = new Address();
    address.postcode = postcode;
    return address;
  }
}
