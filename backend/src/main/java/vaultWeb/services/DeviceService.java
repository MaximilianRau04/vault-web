package vaultWeb.services;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vaultWeb.dtos.DeviceRegistrationRequest;
import vaultWeb.exceptions.UnauthorizedException;
import vaultWeb.models.Device;
import vaultWeb.models.User;
import vaultWeb.repositories.DeviceRepository;
import vaultWeb.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class DeviceService {

  private final DeviceRepository deviceRepository;
  private final UserRepository userRepository;

  public Device registerDevice(DeviceRegistrationRequest request, String username) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UnauthorizedException("User not found"));
    Device device = deviceRepository.findByDeviceId(request.getDeviceId()).orElseGet(Device::new);
    if (device.getUser() != null && !device.getUser().getId().equals(user.getId())) {
      throw new UnauthorizedException("Device already registered to another user");
    }
    device.setDeviceId(request.getDeviceId());
    device.setPublicKey(request.getPublicKey());
    device.setUser(user);
    Instant now = Instant.now();
    if (device.getCreatedAt() == null) {
      device.setCreatedAt(now);
    }
    device.setLastSeen(now);
    return deviceRepository.save(device);
  }
}
