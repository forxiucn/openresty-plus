package net.daoke.openrestyplus.httpconfig;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HttpLocationRepository extends JpaRepository<HttpLocation, UUID> {
    List<HttpLocation> findByServerIdOrderByPath(UUID serverId);
    Optional<HttpLocation> findById(UUID id);
}
