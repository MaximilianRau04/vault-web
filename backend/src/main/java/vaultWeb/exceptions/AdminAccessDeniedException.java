package vaultWeb.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user attempts to perform an action requiring admin privileges but does not have the
 * necessary permissions.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AdminAccessDeniedException extends RuntimeException {

  /**
   * Constructs a new AdminAccessDeniedException for a specific user and group.
   *
   * @param groupId the ID of the group the user tried to access
   * @param userId the ID of the user who lacks admin privileges
   */
  public AdminAccessDeniedException(Long groupId, Long userId) {
    super("User " + userId + " has no admin privileges for group " + groupId);
  }

  /**
   * Constructs a new AdminAccessDeniedException with a custom message.
   *
   * @param message the detail message
   */
  public AdminAccessDeniedException(String message) {
    super(message);
  }
}
