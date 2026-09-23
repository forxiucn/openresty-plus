package net.daoke.openrestyplus.policy;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.util.UUID;

@Entity
@Table(name = "api_policy_rule")
public class ApiPolicyRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    private UUID id;

    @Column(name = "http_method", nullable = false, length = 16)
    private String method;

    @Column(name = "path_pattern", nullable = false, length = 1024)
    private String pathPattern;

    protected ApiPolicyRule() { }

    public ApiPolicyRule(String method, String pathPattern) {
        this.method = method;
        this.pathPattern = pathPattern;
    }

    public String getMethod() { return method; }
    public String getPathPattern() { return pathPattern; }
}
