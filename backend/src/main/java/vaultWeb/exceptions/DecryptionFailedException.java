package vaultWeb.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when decrypt failed. */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class DecryptionFailedException extends RuntimeException {

  /**
   * Constructs a new DecryptionFailedException for a specific message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public DecryptionFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
