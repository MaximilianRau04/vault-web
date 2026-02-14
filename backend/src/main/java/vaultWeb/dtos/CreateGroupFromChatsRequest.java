package vaultWeb.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateGroupFromChatsRequest {
  @NotEmpty(message = "Chat IDs list cannot be empty")
  private List<Long> privateChatIds;

  @NotBlank(message = "Group name cannot be blank")
  private String groupName;

  private String description;
}
