package dev.jefersonsiqueira.clinicflow.auth;

/** A new password that fails {@link PasswordPolicy} — carries which rule failed, in a form fit to show the user. */
public class WeakPasswordException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public WeakPasswordException(String message) {
    super(message);
  }
}
