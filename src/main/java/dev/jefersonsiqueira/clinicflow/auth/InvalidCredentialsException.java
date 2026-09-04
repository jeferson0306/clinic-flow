package dev.jefersonsiqueira.clinicflow.auth;

/**
 * A username that does not exist and a wrong password for one that does are
 * deliberately indistinguishable from outside this class — telling a caller
 * "no such user" is a standing invitation to enumerate every username this
 * system has.
 */
public class InvalidCredentialsException extends RuntimeException {
  public InvalidCredentialsException() {
    super("Invalid username or password");
  }
}
