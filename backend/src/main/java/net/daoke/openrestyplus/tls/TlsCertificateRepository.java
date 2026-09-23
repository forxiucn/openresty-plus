package net.daoke.openrestyplus.tls;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TlsCertificateRepository extends JpaRepository<TlsCertificate, UUID> {
  List<TlsCertificate> findByCenterIdOrderByName(UUID centerId);
  Optional<TlsCertificate> findByIdAndCenterId(UUID id, UUID centerId);
}
