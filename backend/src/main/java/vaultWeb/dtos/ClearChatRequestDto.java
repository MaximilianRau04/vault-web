package vaultWeb.dtos;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ClearChatRequestDto {
  @NotEmpty(message = "Chat IDs cannot be empty")
  private List<Long> privateChatIds;
}
