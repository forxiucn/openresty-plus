package net.daoke.openrestyplus.audit;

import net.daoke.openrestyplus.center.CenterRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/centers/{centerId}/audit-events")
public class AuditEventController {
    private final CenterRepository centers;
    private final AuditEventRepository events;
    public AuditEventController(CenterRepository centers, AuditEventRepository events) { this.centers = centers; this.events = events; }

    @GetMapping
    public List<View> list(@PathVariable UUID centerId) {
        if (!centers.existsById(centerId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found");
        return events.findForCenterOrderByCreatedAtDesc(centerId, centerId.toString()).stream()
            .map(event -> new View(event.getId(), event.getActor(), event.getAction(), event.getResourceType(),
                event.getResourceId(), event.getResult(), event.getCreatedAt())).toList();
    }
    @GetMapping("/paged")
    public net.daoke.openrestyplus.web.PageResult<View> paged(@PathVariable UUID centerId,
                                                              @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
                                                              @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
        return net.daoke.openrestyplus.web.PageResult.of(list(centerId), page, size);
    }

    /** Deliberately omits the JSON detail field because it can contain operational metadata. */
    public record View(UUID id, String actor, String action, String resourceType, String resourceId,
                       String result, Instant createdAt) { }
}
