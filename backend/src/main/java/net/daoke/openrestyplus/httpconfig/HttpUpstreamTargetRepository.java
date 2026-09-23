package net.daoke.openrestyplus.httpconfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface HttpUpstreamTargetRepository extends JpaRepository<HttpUpstreamTarget, UUID> { List<HttpUpstreamTarget> findByUpstreamIdOrderByTargetHostAscTargetPortAsc(UUID upstreamId); }
