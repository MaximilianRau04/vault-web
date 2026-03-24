package vaultWeb.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchOperationDto {
  private boolean success;
  private String message;
  private int affectedCount;
  private Long groupId;
}
