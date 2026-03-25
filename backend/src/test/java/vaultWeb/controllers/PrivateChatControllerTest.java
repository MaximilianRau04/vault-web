package vaultWeb.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import vaultWeb.dtos.DeviceDto;
import vaultWeb.exceptions.UnauthorizedException;
import vaultWeb.exceptions.notfound.PrivateChatNotFoundException;
import vaultWeb.models.Device;
import vaultWeb.models.PrivateChat;
import vaultWeb.models.User;
import vaultWeb.repositories.ChatMessageRepository;
import vaultWeb.repositories.DeviceRepository;
import vaultWeb.repositories.PrivateChatRepository;
import vaultWeb.services.PrivateChatService;

@ExtendWith(MockitoExtension.class)
class PrivateChatControllerTest {

  @Mock private PrivateChatService privateChatService;
  @Mock private ChatMessageRepository chatMessageRepository;
  @Mock private DeviceRepository deviceRepository;
  @Mock private PrivateChatRepository privateChatRepository;

  @InjectMocks private PrivateChatController privateChatController;

  private User createUser(Long id, String username) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    return user;
  }

  @Test
  void shouldGetPrivateChatDevices_WhenUserIsParticipant() {
    User alice = createUser(1L, "alice");
    User bob = createUser(2L, "bob");
    PrivateChat chat = new PrivateChat();
    chat.setId(7L);
    chat.setUser1(alice);
    chat.setUser2(bob);
    Device device = new Device();
    device.setDeviceId("alice-dev-1");
    device.setPublicKey("pk");
    device.setUser(alice);

    Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
    when(authentication.getName()).thenReturn("alice");
    when(privateChatRepository.findById(7L)).thenReturn(Optional.of(chat));
    when(deviceRepository.findByUserIn(List.of(alice, bob))).thenReturn(List.of(device));

    List<DeviceDto> response = privateChatController.getPrivateChatDevices(7L, authentication);

    assertEquals(1, response.size());
    assertEquals("alice-dev-1", response.get(0).getDeviceId());
    verify(deviceRepository, times(1)).findByUserIn(List.of(alice, bob));
  }

  @Test
  void shouldRejectGetPrivateChatDevices_WhenUserIsNotParticipant() {
    User alice = createUser(1L, "alice");
    User bob = createUser(2L, "bob");
    PrivateChat chat = new PrivateChat();
    chat.setId(7L);
    chat.setUser1(alice);
    chat.setUser2(bob);

    Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
    when(authentication.getName()).thenReturn("mallory");
    when(privateChatRepository.findById(7L)).thenReturn(Optional.of(chat));

    assertThrows(
        UnauthorizedException.class,
        () -> privateChatController.getPrivateChatDevices(7L, authentication));
    verify(deviceRepository, times(0)).findByUserIn(any());
  }

  @Test
  void shouldRejectGetPrivateChatDevices_WhenChatNotFound() {
    Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
    when(privateChatRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
        PrivateChatNotFoundException.class,
        () -> privateChatController.getPrivateChatDevices(999L, authentication));
    verify(deviceRepository, times(0)).findByUserIn(any());
  }

  @Test
  void shouldRejectGetPrivateChatDevices_WhenUnauthenticated() {
    assertThrows(
        UnauthorizedException.class, () -> privateChatController.getPrivateChatDevices(7L, null));
    verify(privateChatRepository, times(0)).findById(any());
  }
}
