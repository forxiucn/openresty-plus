package net.daoke.openrestyplus.policy;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ip_policy")
public class IpPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    private UUID id;

    @Column(name = "center_id", nullable = false)
    @JdbcTypeCode(Types.BINARY)
    private UUID centerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PolicyMode mode;

    @Column(nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IpPolicyScope scope;

    @Column(name = "target_resource_id", nullable = false)
    @JdbcTypeCode(Types.BINARY)
    private UUID targetResourceId;

    @Column(nullable = false)
    private boolean enabled;

    @JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "ip_rules", nullable = false, columnDefinition = "json")
    private JsonNode ipRules;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected IpPolicy() { }

    public IpPolicy(UUID centerId, PolicyMode mode, int priority, IpPolicyScope scope,
                    UUID targetResourceId, boolean enabled, JsonNode ipRules) {
        this.centerId = centerId;
        apply(mode, priority, scope, targetResourceId, enabled, ipRules);
    }

    public void apply(PolicyMode mode, int priority, IpPolicyScope scope,
                      UUID targetResourceId, boolean enabled, JsonNode ipRules) {
        this.mode = mode;
        this.priority = priority;
        this.scope = scope;
        this.targetResourceId = targetResourceId;
        this.enabled = enabled;
        this.ipRules = ipRules;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCenterId() { return centerId; }
    public PolicyMode getMode() { return mode; }
    public int getPriority() { return priority; }
    public IpPolicyScope getScope() { return scope; }
    public UUID getTargetResourceId() { return targetResourceId; }
    public boolean isEnabled() { return enabled; }
    public JsonNode getIpRules() { return ipRules; }
    public void changePriority(int priority) { this.priority = priority; this.updatedAt = Instant.now(); }
}
