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
     * Saves a chat message to a group or private chat.
     *
     * <p>The message content is encrypted before being persisted. The sender is
     * identified either by ID or username. If a timestamp is not provided, the
     * current time is used. The message must belong to either a group or a private chat.</p>
     *
     * @param dto DTO containing the message content, sender information, timestamp,
     *            and either a groupId or privateChatId.
     * @return The persisted ChatMessage entity with encrypted content.
     * @throws UserNotFoundException  if the sender cannot be found by ID or username.
     * @throws GroupNotFoundException if neither groupId nor privateChatId is provided,
     *                                or if the specified group/private chat does not exist.
     * @throws RuntimeException       if encryption fails.
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

    /**
     * Decrypts a previously encrypted chat message.
     *
     * <p>Uses the IV and cipher text stored in the database to decrypt
     * and return the original message content as a plain string.</p>
     *
     * @param cipherTextBase64 The encrypted message in Base64 encoding.
     * @param ivBase64         The initialization vector used during encryption, in Base64.
     * @return The decrypted plain text message.
     * @throws RuntimeException if decryption fails.
     */
    public String decrypt(String cipherTextBase64, String ivBase64) {
        try {
            return encryptionUtil.decrypt(cipherTextBase64, ivBase64);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
