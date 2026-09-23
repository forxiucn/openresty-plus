package net.daoke.openrestyplus.node;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NginxNodeRepository extends JpaRepository<NginxNode, UUID> {
    List<NginxNode> findByCenterIdOrderByName(UUID centerId);
}
