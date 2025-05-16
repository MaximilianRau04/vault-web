package meety.models;

import jakarta.persistence.*;
import lombok.*;
import meety.models.enums.Role;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "meety_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private Role role;
}
