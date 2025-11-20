package vaultWeb.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an attempt is made to remove the last admin of a group. This exception ensures that
 * every group always has at least one admin.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LastAdminException extends RuntimeException {

  /**
   * Constructs a LastAdminException for the specified group ID.
   *
   * @param groupId the ID of the group where the last admin removal was attempted
   */
  public LastAdminException(Long groupId) {
    super("Cannot remove the last admin of group " + groupId);
  }
}
