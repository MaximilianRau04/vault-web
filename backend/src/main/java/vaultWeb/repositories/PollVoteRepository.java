package vaultWeb.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vaultWeb.models.Poll;
import vaultWeb.models.PollVote;
import vaultWeb.models.User;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
  boolean existsByOption_PollAndUser(Poll poll, User user);
}
