package net.daoke.openrestyplus.center;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CenterRepository extends JpaRepository<Center, UUID> {}
