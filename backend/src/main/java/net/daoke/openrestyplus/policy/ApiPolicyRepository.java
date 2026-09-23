package net.daoke.openrestyplus.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ApiPolicyRepository extends JpaRepository<ApiPolicy, UUID> {
    List<ApiPolicy> findByCenterIdOrderByPriorityAscIdAsc(UUID centerId);
    List<ApiPolicy> findByCenterIdAndHttpLocationIdOrderByPriorityAscIdAsc(UUID centerId, UUID httpLocationId);
}
