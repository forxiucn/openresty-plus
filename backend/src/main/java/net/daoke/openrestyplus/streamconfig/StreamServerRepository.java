package net.daoke.openrestyplus.streamconfig;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StreamServerRepository extends JpaRepository<StreamServer, UUID> {
  List<StreamServer> findByCenterIdOrderByListenPortAsc(UUID centerId);
  Optional<StreamServer> findByIdAndCenterId(UUID id, UUID centerId);
}
