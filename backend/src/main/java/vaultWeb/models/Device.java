package vaultWeb.models;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Device {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String deviceId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String publicKey;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User user;

  private Instant createdAt;

  private Instant lastSeen;
}
