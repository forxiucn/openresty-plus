package net.daoke.openrestyplus.reload;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ControlApiReloadNodeResultRepository extends JpaRepository<ControlApiReloadNodeResult, UUID> {
    List<ControlApiReloadNodeResult> findByTaskIdOrderByCompletedAtAsc(UUID taskId);
}
