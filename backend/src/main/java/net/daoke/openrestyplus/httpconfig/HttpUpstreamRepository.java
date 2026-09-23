package net.daoke.openrestyplus.httpconfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface HttpUpstreamRepository extends JpaRepository<HttpUpstream, UUID> {
  List<HttpUpstream> findByCenterIdOrderByName(UUID centerId);
  boolean existsByIdAndCenterId(UUID id, UUID centerId);
}
