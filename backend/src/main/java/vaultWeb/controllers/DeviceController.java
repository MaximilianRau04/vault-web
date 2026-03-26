package vaultWeb.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vaultWeb.dtos.DeviceDto;
import vaultWeb.dtos.DeviceRegistrationRequest;
import vaultWeb.exceptions.UnauthorizedException;
import vaultWeb.models.Device;
import vaultWeb.services.DeviceService;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

  private final DeviceService deviceService;

  @PostMapping("/register")
  public DeviceDto registerDevice(
      @Valid @RequestBody DeviceRegistrationRequest request, Authentication authentication) {
    if (authentication == null) {
      throw new UnauthorizedException("User not authenticated");
    }
    Device device = deviceService.registerDevice(request, authentication.getName());
    return DeviceDto.from(device);
  }
}
