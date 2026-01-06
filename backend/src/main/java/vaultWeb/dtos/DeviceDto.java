package vaultWeb.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vaultWeb.models.Device;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDto {
  private String deviceId;
  private String publicKey;
  private Long userId;
  private String username;
  private String createdAt;
  private String lastSeen;

  public static DeviceDto from(Device device) {
    return new DeviceDto(
        device.getDeviceId(),
        device.getPublicKey(),
        device.getUser() != null ? device.getUser().getId() : null,
        device.getUser() != null ? device.getUser().getUsername() : null,
        device.getCreatedAt() != null ? device.getCreatedAt().toString() : null,
        device.getLastSeen() != null ? device.getLastSeen().toString() : null);
  }
}
