package vaultWeb.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {
  @Size(max = 64)
  @NotBlank
  private String deviceId;

  @Size(max = 4096)
  @NotBlank
  private String publicKey;
}
