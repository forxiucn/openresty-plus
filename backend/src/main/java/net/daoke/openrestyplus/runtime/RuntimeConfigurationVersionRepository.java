package net.daoke.openrestyplus.runtime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RuntimeConfigurationVersionRepository extends JpaRepository<RuntimeConfigurationVersion, UUID> {
    List<RuntimeConfigurationVersion> findByCenterIdOrderByVersionNoDesc(UUID centerId);
    Optional<RuntimeConfigurationVersion> findFirstByCenterIdOrderByVersionNoDesc(UUID centerId);
    Optional<RuntimeConfigurationVersion> findByCenterIdAndVersionNo(UUID centerId, long versionNo);
}
