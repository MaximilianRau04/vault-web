package vaultWeb.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user tries to perform poll activities for a optionId that doesn't exist in the
 * respective poll.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PollOptionNotFoundException extends RuntimeException {

  /**
   * Constructs a new PollOptionNotFoundException with a custom message.
   *
   * @param message the detail message explaining the exception
   */
  public PollOptionNotFoundException(String message) {
    super(message);
  }
}
