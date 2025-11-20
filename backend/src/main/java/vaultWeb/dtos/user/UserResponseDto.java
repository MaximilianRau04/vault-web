package vaultWeb.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vaultWeb.models.User;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {
  private String username;

  public UserResponseDto(User user) {
    this.username = user.getUsername();
  }
}
