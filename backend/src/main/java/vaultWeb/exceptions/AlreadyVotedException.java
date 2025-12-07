package vaultWeb.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a user tries to vote for a poll they are already voted in. */
@ResponseStatus(HttpStatus.CONFLICT)
public class AlreadyVotedException extends RuntimeException {

  /**
   * Constructs a new AlreadyVotedException for a specific user and poll.
   *
   * @param pollId the ID of the poll the user tried to vote
   * @param userId the ID of the user who is already voted in
   */
  public AlreadyVotedException(Long pollId, Long userId) {
    super("userId: " + userId + " is already voted for pollId: " + pollId);
  }
}
