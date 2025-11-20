package vaultWeb.exceptions;

/** Thrown when a user attempts to perform an action they are not authorized to perform. */
public class UnauthorizedException extends RuntimeException {

  /**
   * Constructs an UnauthorizedException with the specified detail message.
   *
   * @param message the detail message explaining the reason for the exception
   */
  public UnauthorizedException(String message) {
    super(message);
  }
}
