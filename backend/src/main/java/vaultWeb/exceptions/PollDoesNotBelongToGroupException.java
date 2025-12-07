package vaultWeb.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user tries to perform poll activities for a groupId that doesn't belong to the
 * respective poll.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PollDoesNotBelongToGroupException extends RuntimeException {

  /**
   * Constructs a new PollDoesNotBelongToGroupException with a custom message.
   *
   * @param message the detail message explaining the exception
   */
  public PollDoesNotBelongToGroupException(String message) {
    super(message);
  }
}
