package vaultWeb.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vaultWeb.models.ChatMessage;
import vaultWeb.models.PrivateChat;
import vaultWeb.models.User;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
  List<ChatMessage> findByPrivateChatIdOrderByTimestampAsc(Long privateChatId);

  long countBySender(User sender);

  List<ChatMessage> findTop10BySenderOrderByTimestampDesc(User sender);

  ChatMessage findTop1ByPrivateChatOrderByTimestampDesc(PrivateChat privateChat);

  int deleteByPrivateChat(PrivateChat privateChat);
}
