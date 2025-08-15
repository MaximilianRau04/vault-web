package meety.repositories;

import meety.models.PrivateChat;
import meety.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrivateChatRepository extends JpaRepository<PrivateChat, Long> {
    Optional<PrivateChat> findByUser1AndUser2(User user1, User user2);

    Optional<PrivateChat> findByUser2AndUser1(User user1, User user2);
}
