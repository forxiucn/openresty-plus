package net.daoke.openrestyplus.httpconfig;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import net.daoke.openrestyplus.policy.PolicyModeOrder;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "http_server", uniqueConstraints = @UniqueConstraint(
    name = "uk_http_server_center_domain_port", columnNames = {"center_id", "domain", "listen_port"}))
public class HttpServer {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY)
    private UUID id;
    @Column(name = "center_id", nullable = false) @JdbcTypeCode(Types.BINARY)
    private UUID centerId;
    @Column(nullable = false, length = 255) private String domain;
    @Column(name = "listen_port", nullable = false) private int listenPort;
    @Column(name = "ssl_enabled", nullable = false) private boolean sslEnabled;
    @Column(name = "certificate_id") @JdbcTypeCode(Types.BINARY) private UUID certificateId;
    @Column(name = "upstream_id") @JdbcTypeCode(Types.BINARY) private UUID upstreamId;
    @Column(name = "access_log", nullable = false, length = 512) private String accessLog;
    @Column(name = "error_log", nullable = false, length = 512) private String errorLog;
    @Column(name = "ip_policy_enabled", nullable = false) private boolean ipPolicyEnabled;
    @Column(name = "api_policy_enabled", nullable = false) private boolean apiPolicyEnabled;
    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "ip_policy_mode_order", nullable = false, length = 24)
    private PolicyModeOrder ipPolicyModeOrder = PolicyModeOrder.BLACKLIST_FIRST;
    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "api_policy_mode_order", nullable = false, length = 24)
    private PolicyModeOrder apiPolicyModeOrder = PolicyModeOrder.BLACKLIST_FIRST;
    @Column(name = "root_path", length = 512) private String rootPath;
    @Column(name = "hide_version", nullable = false) private boolean hideVersion = true;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "response_headers", nullable = false, columnDefinition = "json") private List<String> responseHeaders = List.of();
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected HttpServer() { }

    public HttpServer(UUID centerId, String domain, int listenPort, boolean sslEnabled, UUID certificateId, UUID upstreamId,
                      String accessLog, String errorLog) {
        this.centerId = centerId;
        this.domain = domain;
        this.listenPort = listenPort;
        this.sslEnabled = sslEnabled;
        this.certificateId = certificateId;
        this.upstreamId = upstreamId;
        this.accessLog = accessLog;
        this.errorLog = errorLog;
    }
    public void apply(String domain, int listenPort, boolean sslEnabled, UUID certificateId, UUID upstreamId, String accessLog, String errorLog) {
        this.domain = domain; this.listenPort = listenPort; this.sslEnabled = sslEnabled; this.certificateId = certificateId; this.upstreamId = upstreamId;
        this.accessLog = accessLog; this.errorLog = errorLog;
    }

    public UUID getId() { return id; }
    public UUID getCenterId() { return centerId; }
    public String getDomain() { return domain; }
    public int getListenPort() { return listenPort; }
    public boolean isSslEnabled() { return sslEnabled; }
    public UUID getCertificateId() { return certificateId; }
    public UUID getUpstreamId() { return upstreamId; }
    public String getAccessLog() { return accessLog; }
    public String getErrorLog() { return errorLog; }
    public boolean isIpPolicyEnabled() { return ipPolicyEnabled; }
    public boolean isApiPolicyEnabled() { return apiPolicyEnabled; }
    public PolicyModeOrder getIpPolicyModeOrder() { return ipPolicyModeOrder; }
    public PolicyModeOrder getApiPolicyModeOrder() { return apiPolicyModeOrder; }
    public void applyPolicySettings(boolean ipPolicyEnabled, boolean apiPolicyEnabled,
                                    PolicyModeOrder ipPolicyModeOrder, PolicyModeOrder apiPolicyModeOrder) {
        this.ipPolicyEnabled = ipPolicyEnabled;
        this.apiPolicyEnabled = apiPolicyEnabled;
        this.ipPolicyModeOrder = ipPolicyModeOrder;
        this.apiPolicyModeOrder = apiPolicyModeOrder;
    }
    public void applyDirectives(String rootPath, boolean hideVersion, List<String> responseHeaders) { this.rootPath=rootPath; this.hideVersion=hideVersion; this.responseHeaders=List.copyOf(responseHeaders); }
    public String getRootPath(){ return rootPath; } public boolean isHideVersion(){ return hideVersion; } public List<String> getResponseHeaders(){ return responseHeaders; }
}
