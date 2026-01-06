package vaultWeb.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vaultWeb.dtos.ChatMessageDto;
import vaultWeb.dtos.DeviceDto;
import vaultWeb.dtos.PrivateChatDto;
import vaultWeb.exceptions.UnauthorizedException;
import vaultWeb.models.ChatMessage;
import vaultWeb.models.PrivateChat;
import vaultWeb.models.User;
import vaultWeb.repositories.ChatMessageRepository;
import vaultWeb.repositories.DeviceRepository;
import vaultWeb.repositories.PrivateChatRepository;
import vaultWeb.services.PrivateChatService;

@RestController
@RequestMapping("/api/private-chats")
@Tag(
    name = "Private Chat Controller",
    description =
        "Handles private chats between users, including chat creation and message retrieval")
@RequiredArgsConstructor
public class PrivateChatController {

  private final PrivateChatService privateChatService;
  private final ChatMessageRepository chatMessageRepository;
  private final DeviceRepository deviceRepository;
  private final PrivateChatRepository privateChatRepository;

  @GetMapping("/between")
  @Operation(
      summary = "Get or create a private chat between two users",
      description =
          """
                    This endpoint retrieves an existing private chat between two users, or creates a new one if it does not exist.
                    - 'sender' and 'receiver' are the usernames of the users.
                    - Returns a PrivateChatDto containing the chat ID and the usernames of both participants.
                    """)
  public PrivateChatDto getOrCreatePrivateChat(
      @RequestParam String sender, @RequestParam String receiver) {
    PrivateChat chat = privateChatService.getOrCreatePrivateChat(sender, receiver);
    return new PrivateChatDto(
        chat.getId(), chat.getUser1().getUsername(), chat.getUser2().getUsername());
  }

  @GetMapping("/private")
  @Operation(
      summary = "Get all messages of a private chat",
      description =
          """
                    Retrieves all messages from a specific private chat.
                    - 'privateChatId' is the ID of the private chat.
                    - Messages are ordered chronologically by timestamp.
                    - The message content is end-to-end encrypted and never decrypted by the server.
                    - Returns a list of ChatMessageDto containing encrypted payload, sender info, timestamp, and chat ID.
                    """)
  public List<ChatMessageDto> getPrivateChatMessages(@RequestParam Long privateChatId) {
    List<ChatMessage> messages =
        chatMessageRepository.findByPrivateChatIdOrderByTimestampAsc(privateChatId);

    return messages.stream()
        .map(
            message -> {
              ChatMessageDto dto = new ChatMessageDto();
              dto.setE2eePayload(message.getE2eePayload());
              dto.setTimestamp(message.getTimestamp().toString());
              dto.setGroupId(null);
              dto.setPrivateChatId(privateChatId);
              dto.setSenderId(message.getSender().getId());
              dto.setSenderUsername(message.getSender().getUsername());
              dto.setSenderDeviceId(message.getSenderDeviceId());
              return dto;
            })
        .toList();
  }

  @GetMapping("/devices")
  public List<DeviceDto> getPrivateChatDevices(
      @RequestParam Long privateChatId, Authentication authentication) {
    PrivateChat chat =
        privateChatRepository
            .findById(privateChatId)
            .orElseThrow(() -> new IllegalArgumentException("Private chat not found"));
    String username = authentication.getName();
    boolean isParticipant =
        (chat.getUser1() != null && username.equals(chat.getUser1().getUsername()))
            || (chat.getUser2() != null && username.equals(chat.getUser2().getUsername()));
    if (!isParticipant) {
      throw new UnauthorizedException("Not allowed to access devices for this chat");
    }
    List<User> users = new ArrayList<>();
    if (chat.getUser1() != null) {
      users.add(chat.getUser1());
    }
    if (chat.getUser2() != null) {
      users.add(chat.getUser2());
    }
    return deviceRepository.findByUserIn(users).stream().map(DeviceDto::from).toList();
  }
}
