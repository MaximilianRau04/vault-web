package vaultWeb.services;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vaultWeb.exceptions.notfound.UserNotFoundException;
import vaultWeb.models.PrivateChat;
import vaultWeb.models.User;
import vaultWeb.repositories.PrivateChatRepository;
import vaultWeb.repositories.UserRepository;

/**
 * Service class for handling private chats between two users.
 *
 * <p>Provides functionality to retrieve an existing private chat or create a new one if it does not
 * exist.
 */
@Service
@RequiredArgsConstructor
public class PrivateChatService {

  private final PrivateChatRepository privateChatRepository;
  private final UserRepository userRepository;

  /**
   * Retrieves an existing private chat between two users or creates a new one if none exists.
   *
   * @param username1 The username of the first user.
   * @param username2 The username of the second user.
   * @return The existing or newly created PrivateChat entity.
   * @throws UserNotFoundException if either user does not exist.
   */
  public PrivateChat getOrCreatePrivateChat(String username1, String username2) {
    // Find the first user by username, throw exception if not found
    User user1 =
        userRepository
            .findByUsername(username1)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + username1));

    // Find the second user by username, throw exception if not found
    User user2 =
        userRepository
            .findByUsername(username2)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + username2));

    // Check if a private chat already exists in one direction (user1 -> user2)
    Optional<PrivateChat> chatOpt = privateChatRepository.findByUser1AndUser2(user1, user2);
    if (chatOpt.isPresent()) return chatOpt.get();

    // Check if a private chat exists in the opposite direction (user2 -> user1)
    chatOpt = privateChatRepository.findByUser2AndUser1(user1, user2);
    if (chatOpt.isPresent()) return chatOpt.get();

    // If no chat exists, create a new PrivateChat
    PrivateChat privateChat = new PrivateChat();
    privateChat.setUser1(user1);
    privateChat.setUser2(user2);

    // Save and return the new private chat
    return privateChatRepository.save(privateChat);
  }
}
