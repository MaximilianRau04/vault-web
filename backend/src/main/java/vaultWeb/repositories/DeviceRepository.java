package vaultWeb.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vaultWeb.models.Device;
import vaultWeb.models.User;

public interface DeviceRepository extends JpaRepository<Device, Long> {
  Optional<Device> findByDeviceId(String deviceId);

  List<Device> findByUserIn(Collection<User> users);
}
