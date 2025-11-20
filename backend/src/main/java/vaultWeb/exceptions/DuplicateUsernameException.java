package vaultWeb.exceptions;

/** Thrown when a user tries to register with a username that already exists. */
public class DuplicateUsernameException extends RuntimeException {

  /**
   * Constructs a new DuplicateUsernameException with a custom message.
   *
   * @param message the detail message explaining the exception
   */
  public DuplicateUsernameException(String message) {
    super(message);
  }
}
