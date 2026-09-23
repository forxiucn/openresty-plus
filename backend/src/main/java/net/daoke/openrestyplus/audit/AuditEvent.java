package net.daoke.openrestyplus.audit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY)
    private UUID id;
    @Column(nullable = false, length = 128) private String actor;
    @Column(nullable = false, length = 128) private String action;
    @Column(name = "resource_type", nullable = false, length = 64) private String resourceType;
    @Column(name = "resource_id", nullable = false, length = 128) private String resourceId;
    @Column(name = "center_id") @JdbcTypeCode(Types.BINARY) private UUID centerId;
    @Column(nullable = false, length = 32) private String result;
    @Column(name = "request_id", length = 128) private String requestId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "json") private JsonNode detail;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected AuditEvent() { }
    public AuditEvent(String actor, String action, String resourceType, String resourceId, String result, JsonNode detail) {
        this.actor = actor; this.action = action; this.resourceType = resourceType; this.resourceId = resourceId;
        this.result = result; this.detail = detail;
    }
    public AuditEvent(UUID centerId, String actor, String action, String resourceType, String resourceId,
                      String result, JsonNode detail) {
        this(actor, action, resourceType, resourceId, result, detail);
        this.centerId = centerId;
    }
    public UUID getId() { return id; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public UUID getCenterId() { return centerId; }
    public String getResult() { return result; }
    public JsonNode getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}
