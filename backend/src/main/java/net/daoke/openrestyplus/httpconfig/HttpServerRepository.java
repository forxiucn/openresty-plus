package net.daoke.openrestyplus.httpconfig;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HttpServerRepository extends JpaRepository<HttpServer, UUID> {
    List<HttpServer> findByCenterIdOrderByDomainAscListenPortAsc(UUID centerId);
    Optional<HttpServer> findByIdAndCenterId(UUID id, UUID centerId);
}
