package net.daoke.openrestyplus.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/** Records successful control-plane mutations without copying request payloads or credentials. */
@Service
public class AuditService {
    private static final String SYSTEM_ACTOR = "admin";

    private final AuditEventRepository events;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository events) {
        this.events = events;
        this.objectMapper = new ObjectMapper();
    }

    public void success(UUID centerId, String action, String resourceType, UUID resourceId) {
        record(centerId, action, resourceType, resourceId, "SUCCESS", Map.of());
    }

    public void success(UUID centerId, String action, String resourceType, UUID resourceId,
                        Map<String, ?> changeDetails) {
        record(centerId, action, resourceType, resourceId, "SUCCESS", changeDetails);
    }

    public void failure(UUID centerId, String action, String resourceType, UUID resourceId) {
        record(centerId, action, resourceType, resourceId, "FAILED", Map.of());
    }

    private void record(UUID centerId, String action, String resourceType, UUID resourceId, String result,
                        Map<String, ?> changeDetails) {
        ObjectNode detail = objectMapper.valueToTree(changeDetails);
        if (centerId != null) {
            detail.put("centerId", centerId.toString());
        }
        detail.put("resourceId", resourceId.toString());
        events.save(new AuditEvent(centerId, SYSTEM_ACTOR, action, resourceType, resourceId.toString(), result, detail));
    }
}
