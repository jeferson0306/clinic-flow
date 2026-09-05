package dev.jefersonsiqueira.clinicflow.common.validation;

/**
 * The one place {@code fullName}'s shape is defined, so every request record
 * that carries one — patient, doctor — enforces the same rule rather than
 * each re-deciding what a name looks like. Letters (any script, via
 * {@code \p{L}}) and combining marks (accents as separate codepoints, via
 * {@code \p{M}}) plus space, apostrophe, period and hyphen for names like
 * "Mary-Jane O'Brien" or "Off. J. Silva" — never a digit, which is the gap
 * that let a CPF-shaped string through as somebody's name before this
 * existed. Length bounds match nothing more principled than "clearly not
 * accidental one-character input" and "clearly not pasted garbage."
 */
public final class NamePattern {

  public static final String REGEXP = "^[\\p{L}\\p{M} '.-]{3,120}$";

  public static final String MESSAGE =
      "must be 3-120 characters of letters, spaces, apostrophes, periods or hyphens only";

  private NamePattern() {}
}
