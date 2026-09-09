package dev.jefersonsiqueira.clinicflow.auth;

/**
 * An email that does not exist and a wrong password for one that does are
 * deliberately indistinguishable from outside this class — telling a caller
 * "no such user" is a standing invitation to enumerate every email this
 * system has.
 */
public class InvalidCredentialsException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InvalidCredentialsException() {
    super("Invalid email or password");
  }
}
