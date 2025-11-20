package vaultWeb.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vaultWeb.models.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {}
