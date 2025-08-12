package meety.services;

import lombok.RequiredArgsConstructor;
import meety.dtos.ChatMessageDto;
import meety.exceptions.notfound.GroupNotFoundException;
import meety.exceptions.notfound.UserNotFoundException;
import meety.models.ChatMessage;
import meety.models.Group;
import meety.models.PrivateChat;
import meety.models.User;
import meety.repositories.ChatMessageRepository;
import meety.repositories.GroupRepository;
import meety.repositories.PrivateChatRepository;
import meety.repositories.UserRepository;
import meety.security.EncryptionUtil;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PrivateChatRepository privateChatRepository;
    private final EncryptionUtil encryptionUtil;

    /**
     * Saves a chat message to the database.
     *
     * @param dto DTO containing the content, sender ID, and group ID.
     * @return The persisted ChatMessage entity.
     */
    public ChatMessage saveMessage(ChatMessageDto dto) {
        User sender;

        if (dto.getSenderId() != null) {
            sender = userRepository.findById(dto.getSenderId())
                    .orElseThrow(() -> new UserNotFoundException("Sender not found by ID"));
        } else if (dto.getSenderUsername() != null) {
            sender = userRepository.findByUsername(dto.getSenderUsername())
                    .orElseThrow(() -> new UserNotFoundException("Sender not found by username"));
        } else {
            throw new UserNotFoundException("Sender information missing");
        }

        EncryptionUtil.EncryptResult encrypted;
        try {
            encrypted = encryptionUtil.encrypt(dto.getContent());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }

        ChatMessage message = new ChatMessage();
        message.setCipherText(encrypted.cipherTextBase64);
        message.setIv(encrypted.ivBase64);
        message.setSender(sender);

        if (dto.getTimestamp() != null) {
            message.setTimestamp(Instant.parse(dto.getTimestamp()));
        } else {
            message.setTimestamp(Instant.now());
        }

        if (dto.getGroupId() != null) {
            Group group = groupRepository.findById(dto.getGroupId())
                    .orElseThrow(() -> new GroupNotFoundException("Group with id " + dto.getGroupId() + " not found"));
            message.setGroup(group);
        } else if (dto.getPrivateChatId() != null) {
            PrivateChat privateChat = privateChatRepository.findById(dto.getPrivateChatId())
                    .orElseThrow(() -> new GroupNotFoundException("PrivateChat not found"));
            message.setPrivateChat(privateChat);
        } else {
            throw new GroupNotFoundException("Either groupId or privateChatId must be provided");
        }

        return chatMessageRepository.save(message);
    }
}
