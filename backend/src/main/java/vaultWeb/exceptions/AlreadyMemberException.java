package vaultWeb.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a user tries to join a group they are already a member of. */
@ResponseStatus(HttpStatus.CONFLICT)
public class AlreadyMemberException extends RuntimeException {

  /**
   * Constructs a new AlreadyMemberException for a specific user and group.
   *
   * @param groupId the ID of the group the user tried to join
   * @param userId the ID of the user who is already a member
   */
  public AlreadyMemberException(Long groupId, Long userId) {
    super("User " + userId + " is already a member of group " + groupId);
  }
}
