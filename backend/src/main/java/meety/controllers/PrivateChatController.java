package meety.controllers;

import lombok.RequiredArgsConstructor;
import meety.dtos.ChatMessageDto;
import meety.dtos.PrivateChatDto;
import meety.models.ChatMessage;
import meety.models.PrivateChat;
import meety.repositories.ChatMessageRepository;
import meety.security.EncryptionUtil;
import meety.services.PrivateChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/private-chats")
@RequiredArgsConstructor
public class PrivateChatController {

    private final PrivateChatService privateChatService;
    private final ChatMessageRepository chatMessageRepository;
    private final EncryptionUtil encryptionUtil;

    @GetMapping("/between")
    public PrivateChatDto getOrCreatePrivateChat(
            @RequestParam String sender,
            @RequestParam String receiver
    ) {
        PrivateChat chat = privateChatService.getOrCreatePrivateChat(sender, receiver);
        return new PrivateChatDto(chat.getId(), chat.getUser1().getUsername(), chat.getUser2().getUsername());
    }

    @GetMapping("/private")
    public List<ChatMessageDto> getPrivateChatMessages(@RequestParam Long privateChatId) {
        List<ChatMessage> messages = chatMessageRepository.findByPrivateChatIdOrderByTimestampAsc(privateChatId);

        return messages.stream()
                .map(message -> {
                    try {
                        String decryptedContent = encryptionUtil.decrypt(message.getCipherText(), message.getIv());
                        return new ChatMessageDto(
                                decryptedContent,
                                message.getTimestamp().toString(),
                                null,
                                privateChatId,
                                message.getSender().getId(),
                                message.getSender().getUsername()
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Decryption failed", e);
                    }
                })
                .toList();
    }
}
