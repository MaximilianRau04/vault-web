package vaultWeb.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vaultWeb.exceptions.notfound.UserNotFoundException;
import vaultWeb.models.PrivateChat;
import vaultWeb.models.User;
import vaultWeb.repositories.PrivateChatRepository;
import vaultWeb.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class PrivateChatServiceTest {

  @Mock private PrivateChatRepository privateChatRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private PrivateChatService privateChatService;

  private User createUser(Long id, String username) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    return user;
  }

  private PrivateChat createPrivateChat(Long id, User user1, User user2) {
    PrivateChat chat = new PrivateChat();
    chat.setId(id);
    chat.setUser1(user1);
    chat.setUser2(user2);
    return chat;
  }

  @Test
  void shouldReturnExistingChat_WhenChatExistsUser1ToUser2() {
    User user1 = createUser(1L, "alice");
    User user2 = createUser(2L, "bob");
    PrivateChat existingChat = createPrivateChat(10L, user1, user2);

    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user1));
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user2));
    when(privateChatRepository.findByUser1AndUser2(user1, user2))
        .thenReturn(Optional.of(existingChat));

    PrivateChat result = privateChatService.getOrCreatePrivateChat("alice", "bob");

    assertNotNull(result);
    assertEquals(10L, result.getId());
    assertEquals(user1, result.getUser1());
    assertEquals(user2, result.getUser2());
    verify(privateChatRepository, never()).save(any());
  }

  @Test
  void shouldReturnExistingChat_WhenChatExistsUser2ToUser1() {
    User user1 = createUser(1L, "alice");
    User user2 = createUser(2L, "bob");
    PrivateChat existingChat = createPrivateChat(10L, user2, user1);

    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user1));
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user2));
    when(privateChatRepository.findByUser1AndUser2(user1, user2)).thenReturn(Optional.empty());
    when(privateChatRepository.findByUser2AndUser1(user1, user2))
        .thenReturn(Optional.of(existingChat));

    PrivateChat result = privateChatService.getOrCreatePrivateChat("alice", "bob");

    assertNotNull(result);
    assertEquals(10L, result.getId());
    verify(privateChatRepository, never()).save(any());
  }

  @Test
  void shouldCreateNewChat_WhenNoChatExists() {
    User user1 = createUser(1L, "alice");
    User user2 = createUser(2L, "bob");

    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user1));
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user2));
    when(privateChatRepository.findByUser1AndUser2(user1, user2)).thenReturn(Optional.empty());
    when(privateChatRepository.findByUser2AndUser1(user1, user2)).thenReturn(Optional.empty());
    when(privateChatRepository.save(any(PrivateChat.class)))
        .thenAnswer(
            invocation -> {
              PrivateChat chat = invocation.getArgument(0);
              chat.setId(100L);
              return chat;
            });

    PrivateChat result = privateChatService.getOrCreatePrivateChat("alice", "bob");

    assertNotNull(result);
    assertEquals(100L, result.getId());
    assertEquals(user1, result.getUser1());
    assertEquals(user2, result.getUser2());
    verify(privateChatRepository).save(any(PrivateChat.class));
  }

  @Test
  void shouldFailGetOrCreate_WhenFirstUserNotFound() {
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThrows(
        UserNotFoundException.class,
        () -> privateChatService.getOrCreatePrivateChat("unknown", "bob"));

    verify(privateChatRepository, never()).findByUser1AndUser2(any(), any());
    verify(privateChatRepository, never()).save(any());
  }

  @Test
  void shouldFailGetOrCreate_WhenSecondUserNotFound() {
    User user1 = createUser(1L, "alice");

    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user1));
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThrows(
        UserNotFoundException.class,
        () -> privateChatService.getOrCreatePrivateChat("alice", "unknown"));

    verify(privateChatRepository, never()).findByUser1AndUser2(any(), any());
    verify(privateChatRepository, never()).save(any());
  }
}
