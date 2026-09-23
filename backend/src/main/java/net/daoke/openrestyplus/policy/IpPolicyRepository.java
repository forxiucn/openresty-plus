package net.daoke.openrestyplus.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IpPolicyRepository extends JpaRepository<IpPolicy, UUID> {
    List<IpPolicy> findByCenterIdOrderByPriorityAscIdAsc(UUID centerId);
    List<IpPolicy> findByCenterIdAndScopeAndTargetResourceIdOrderByPriorityAscIdAsc(
        UUID centerId, IpPolicyScope scope, UUID targetResourceId);
}
