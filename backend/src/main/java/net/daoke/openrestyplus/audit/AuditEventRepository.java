package net.daoke.openrestyplus.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(String resourceType, String resourceId);

    @Query("""
        select event from AuditEvent event
        where event.centerId = :centerId
           or (event.resourceType = 'CENTER' and event.resourceId = :centerResourceId)
        order by event.createdAt desc
        """)
    List<AuditEvent> findForCenterOrderByCreatedAtDesc(@Param("centerId") UUID centerId,
                                                        @Param("centerResourceId") String centerResourceId);
}
