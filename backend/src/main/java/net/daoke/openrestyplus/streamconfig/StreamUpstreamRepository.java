package net.daoke.openrestyplus.streamconfig;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StreamUpstreamRepository extends JpaRepository<StreamUpstream, UUID> {
  List<StreamUpstream> findByCenterIdOrderByName(UUID centerId);
  Optional<StreamUpstream> findByIdAndCenterId(UUID id, UUID centerId);
}
