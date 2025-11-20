package vaultWeb.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import vaultWeb.dtos.user.UserDto;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "vault_user")
@ToString(exclude = "groupMemberships")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String username;
  private String password;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private List<GroupMember> groupMemberships;

  public User(UserDto userDto) {
    username = userDto.getUsername();
    password = userDto.getPassword();
    groupMemberships = List.of();
  }
}
