package net.daoke.openrestyplus.dns;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DnsResolverConfigurationRepository extends JpaRepository<DnsResolverConfiguration,UUID>{ List<DnsResolverConfiguration> findByCenterIdOrderByScopeAsc(UUID centerId); Optional<DnsResolverConfiguration> findByCenterIdAndScopeAndTargetResourceId(UUID centerId,DnsResolverScope scope,UUID targetResourceId); }
