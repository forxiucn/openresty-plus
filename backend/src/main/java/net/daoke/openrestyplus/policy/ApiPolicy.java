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
    @Column(name = "http_location_id", nullable = false) @JdbcTypeCode(Types.BINARY)
    private UUID httpLocationId;
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
    public ApiPolicy(UUID centerId, PolicyMode mode, int priority, UUID httpLocationId,
                     boolean enabled, List<ApiPolicyRule> rules) {
        this.centerId = centerId;
        apply(mode, priority, httpLocationId, enabled, rules);
    }
    public void apply(PolicyMode mode, int priority, UUID httpLocationId,
                      boolean enabled, List<ApiPolicyRule> rules) {
        this.mode = mode; this.priority = priority; this.httpLocationId = httpLocationId;
        this.enabled = enabled; this.rules.clear(); this.rules.addAll(rules); this.updatedAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getCenterId() { return centerId; }
    public PolicyMode getMode() { return mode; }
    public int getPriority() { return priority; }
    public UUID getHttpLocationId() { return httpLocationId; }
    public boolean isEnabled() { return enabled; }
    public List<ApiPolicyRule> getRules() { return List.copyOf(rules); }
}
