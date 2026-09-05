package dev.jefersonsiqueira.clinicflow.validation.brdoc;

/**
 * A document brdoc rejected. Carries the field name so a caller can point the
 * error back at the right form field, and brdoc's own message — brdoc already
 * says why in a form fit to show someone, so this does not write a second one.
 */
public class DocumentValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String field;

  public DocumentValidationException(String field, String brdocMessage) {
    super(brdocMessage);
    this.field = field;
  }

  public String field() {
    return field;
  }
}
