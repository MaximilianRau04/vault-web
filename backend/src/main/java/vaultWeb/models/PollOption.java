package vaultWeb.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "poll_options")
public class PollOption {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "poll_id", nullable = false)
  @JsonBackReference
  private Poll poll;

  private String text;

  @OneToMany(mappedBy = "option", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonManagedReference
  private List<PollVote> votes;
}
