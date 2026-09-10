package dev.jefersonsiqueira.clinicflow.auth;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * One place the "what counts as a strong enough password" rule lives, so a
 * future second caller (a real signup flow, an admin-resets-a-user's-password
 * endpoint) enforces the identical rule rather than a slightly different
 * regex copied from here. Checked server-side regardless of what the
 * frontend already blocked — the frontend's copy of this rule is UX, this
 * one is the actual boundary, the same split every other validated field in
 * this app already follows.
 *
 * Length over composition, per current guidance (NIST SP 800-63B, OWASP
 * ASVS v5): forcing every character class is the older model and mostly
 * pushes people toward predictable patterns rather than genuinely stronger
 * passwords, but it is not *wrong* either, so this keeps it rather than
 * dropping it — a length bump plus a real breach check (not a length swap
 * for one) is the improvement actually worth making. The breach check is
 * the newer, more effective half of the current recommendation: a password
 * that shows up in a known leak is a real risk no composition rule catches
 * at all.
 */
@ApplicationScoped
public class PasswordPolicy {

  static final int MIN_LENGTH = 12;

  @Inject @RestClient PwnedPasswordChecker pwnedPasswords;

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
    if (isKnownBreached(password)) {
      throw new WeakPasswordException("This password has appeared in a known data breach — choose a different one");
    }
  }

  /**
   * Fails open: a third-party service being slow or down can never block a
   * legitimate password change, the same stance {@link
   * dev.jefersonsiqueira.clinicflow.address.AddressLookupService} already
   * takes on ViaCEP. A breach check that sometimes doesn't run is still
   * strictly better than never running one.
   */
  private boolean isKnownBreached(String password) {
    try {
      String hash = sha1Hex(password);
      String prefix = hash.substring(0, 5);
      String suffix = hash.substring(5);
      String body = pwnedPasswords.range(prefix);
      return body.lines().anyMatch(line -> line.regionMatches(true, 0, suffix, 0, suffix.length()));
    } catch (RuntimeException e) {
      Log.warnf("Have I Been Pwned check failed, continuing without it: %s", e.getMessage());
      return false;
    }
  }

  private static String sha1Hex(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        hex.append(String.format("%02X", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      // SHA-1 is a JDK-guaranteed algorithm — see AuthService's identical
      // reasoning for SHA-256 there.
      throw new IllegalStateException("SHA-1 unavailable", e);
    }
  }
}
