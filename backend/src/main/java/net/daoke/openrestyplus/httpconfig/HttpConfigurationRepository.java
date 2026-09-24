package net.daoke.openrestyplus.httpconfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface HttpConfigurationRepository extends JpaRepository<HttpConfiguration, UUID> { }
