package net.daoke.openrestyplus.reload;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ControlApiReloadTaskRepository extends JpaRepository<ControlApiReloadTask, UUID> {
    List<ControlApiReloadTask> findByCenterIdOrderByCreatedAtDesc(UUID centerId);
}
