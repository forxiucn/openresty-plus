package net.daoke.openrestyplus.policy;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "api_policy")
public class ApiPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    private UUID id;
    @Column(name = "center_id", nullable = false) @JdbcTypeCode(Types.BINARY)
    private UUID centerId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private PolicyMode mode;
    @Column(nullable = false)
    private int priority;
    /** Retained for migrated historical rows; new code uses scope + targetResourceId. */
    @Column(name = "http_location_id") @JdbcTypeCode(Types.BINARY)
    private UUID httpLocationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private ApiPolicyScope scope;
    @Column(name = "target_resource_id", nullable = false) @JdbcTypeCode(Types.BINARY)
    private UUID targetResourceId;
    @Column(nullable = false)
    private boolean enabled;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "api_policy_id", nullable = false)
    @OrderColumn(name = "rule_order")
    private List<ApiPolicyRule> rules = new ArrayList<>();
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ApiPolicy() { }
    public ApiPolicy(UUID centerId, PolicyMode mode, int priority, ApiPolicyScope scope, UUID targetResourceId,
                     boolean enabled, List<ApiPolicyRule> rules) {
        this.centerId = centerId; this.scope = scope; this.targetResourceId = targetResourceId;
        apply(mode, priority, scope, targetResourceId, enabled, rules);
    }
    public void apply(PolicyMode mode, int priority, ApiPolicyScope scope, UUID targetResourceId,
                      boolean enabled, List<ApiPolicyRule> rules) {
        this.mode = mode; this.priority = priority; this.scope = scope; this.targetResourceId = targetResourceId;
        this.httpLocationId = scope == ApiPolicyScope.HTTP_LOCATION ? targetResourceId : null;
        this.enabled = enabled; this.rules.clear(); this.rules.addAll(rules); this.updatedAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getCenterId() { return centerId; }
    public PolicyMode getMode() { return mode; }
    public int getPriority() { return priority; }
    public UUID getHttpLocationId() { return httpLocationId; }
    public ApiPolicyScope getScope() { return scope; }
    public UUID getTargetResourceId() { return targetResourceId; }
    public boolean isEnabled() { return enabled; }
    public List<ApiPolicyRule> getRules() { return List.copyOf(rules); }
    public void changePriority(int priority) { this.priority = priority; this.updatedAt = Instant.now(); }
}
