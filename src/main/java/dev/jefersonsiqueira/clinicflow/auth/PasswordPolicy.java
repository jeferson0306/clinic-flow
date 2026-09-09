package dev.jefersonsiqueira.clinicflow.auth;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * One place the "what counts as a strong enough password" rule lives, so a
 * future second caller (a real signup flow, an admin-resets-a-user's-password
 * endpoint) enforces the identical rule rather than a slightly different
 * regex copied from here. Checked server-side regardless of what the
 * frontend already blocked — the frontend's copy of this rule is UX, this
 * one is the actual boundary, the same split every other validated field in
 * this app already follows.
 */
@ApplicationScoped
public class PasswordPolicy {

  static final int MIN_LENGTH = 10;

  public void validate(String password) {
    if (password.length() < MIN_LENGTH) {
      throw new WeakPasswordException("Password must be at least " + MIN_LENGTH + " characters long");
    }
    if (password.chars().noneMatch(Character::isUpperCase)) {
      throw new WeakPasswordException("Password must contain at least one uppercase letter");
    }
    if (password.chars().noneMatch(Character::isLowerCase)) {
      throw new WeakPasswordException("Password must contain at least one lowercase letter");
    }
    if (password.chars().noneMatch(Character::isDigit)) {
      throw new WeakPasswordException("Password must contain at least one digit");
    }
    if (password.chars().allMatch(Character::isLetterOrDigit)) {
      throw new WeakPasswordException("Password must contain at least one special character");
    }
  }
}
