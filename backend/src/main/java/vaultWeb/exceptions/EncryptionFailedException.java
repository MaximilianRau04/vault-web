package vaultWeb.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when encrypt failed. */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EncryptionFailedException extends RuntimeException {

  /**
   * Constructs a new EncryptionFailedException for a specific message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public EncryptionFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
