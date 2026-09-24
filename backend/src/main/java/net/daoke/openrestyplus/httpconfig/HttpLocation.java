package net.daoke.openrestyplus.httpconfig;

import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "http_location", uniqueConstraints = @UniqueConstraint(
    name = "uk_http_location_server_path", columnNames = {"server_id", "path"}))
public class HttpLocation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY)
    private UUID id;
    @Column(name = "server_id", nullable = false) @JdbcTypeCode(Types.BINARY)
    private UUID serverId;
    @Column(nullable = false, length = 512) private String path;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "json")
    private List<String> methods;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "content_types", nullable = false, columnDefinition = "json")
    private List<String> contentTypes;
    @Column(name = "header_length_min", nullable = false) private int headerLengthMin;
    @Column(name = "header_length_max", nullable = false) private int headerLengthMax;
    @Column(name = "body_length_min", nullable = false) private long bodyLengthMin;
    @Column(name = "body_length_max", nullable = false) private long bodyLengthMax;
    @Column(name = "upstream_id", nullable = false) @JdbcTypeCode(Types.BINARY)
    private UUID upstreamId;
    @Column(name = "proxy_connect_timeout_ms", nullable = false) private int proxyConnectTimeoutMs;
    @Column(name = "proxy_read_timeout_ms", nullable = false) private int proxyReadTimeoutMs;
    @Column(name = "proxy_send_timeout_ms", nullable = false) private int proxySendTimeoutMs;
    @Column(name = "rate_limit_enabled", nullable = false) private boolean rateLimitEnabled;
    @Column(name = "rate_per_second", nullable = false) private int ratePerSecond;
    @Column(name = "rate_limit_burst", nullable = false) private int rateLimitBurst;
    @Column(name = "rate_limit_nodelay", nullable = false) private boolean rateLimitNodelay;
    @Column(name = "dynamic_dns_enabled", nullable = false) private boolean dynamicDnsEnabled;
    @Column(name = "dynamic_dns_host") private String dynamicDnsHost;
    @Column(name = "dynamic_dns_port") private Integer dynamicDnsPort;
    @Column(name = "ip_policy_enabled", nullable = false) private boolean ipPolicyEnabled;
    @Column(name = "api_policy_enabled", nullable = false) private boolean apiPolicyEnabled;
    @Enumerated(jakarta.persistence.EnumType.STRING) @Column(nullable = false, length = 16) private LocationAction action = LocationAction.PROXY;
    @Column(name = "root_path", length = 512) private String rootPath;
    @Column(name = "alias_path", length = 512) private String aliasPath;
    @Column(name = "return_status") private Integer returnStatus;
    @Column(name = "return_body", columnDefinition = "text") private String returnBody;
    @Enumerated(jakarta.persistence.EnumType.STRING) @Column(name = "return_content_type_mode", nullable = false, length = 16) private ReturnContentTypeMode returnContentTypeMode = ReturnContentTypeMode.CUSTOM;
    @Column(name = "return_content_type", length = 255) private String returnContentType;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "response_headers", nullable = false, columnDefinition = "json") private List<String> responseHeaders = List.of();
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected HttpLocation() { }

    public HttpLocation(UUID serverId, String path, List<String> methods, List<String> contentTypes,
                        int headerLengthMin, int headerLengthMax, long bodyLengthMin, long bodyLengthMax,
                        UUID upstreamId, int proxyConnectTimeoutMs, int proxyReadTimeoutMs, int proxySendTimeoutMs,
                        boolean rateLimitEnabled, int ratePerSecond, int rateLimitBurst, boolean rateLimitNodelay) {
        this.serverId = serverId;
        this.path = path;
        this.methods = List.copyOf(methods);
        this.contentTypes = List.copyOf(contentTypes);
        this.headerLengthMin = headerLengthMin;
        this.headerLengthMax = headerLengthMax;
        this.bodyLengthMin = bodyLengthMin;
        this.bodyLengthMax = bodyLengthMax;
        this.upstreamId = upstreamId;
        this.proxyConnectTimeoutMs = proxyConnectTimeoutMs;
        this.proxyReadTimeoutMs = proxyReadTimeoutMs;
        this.proxySendTimeoutMs = proxySendTimeoutMs;
        this.rateLimitEnabled = rateLimitEnabled; this.ratePerSecond = ratePerSecond;
        this.rateLimitBurst = rateLimitBurst; this.rateLimitNodelay = rateLimitNodelay;
    }
    public void apply(String path, List<String> methods, List<String> contentTypes,
                      int headerLengthMin, int headerLengthMax, long bodyLengthMin, long bodyLengthMax,
                      UUID upstreamId, int proxyConnectTimeoutMs, int proxyReadTimeoutMs, int proxySendTimeoutMs,
                      boolean rateLimitEnabled, int ratePerSecond, int rateLimitBurst, boolean rateLimitNodelay) {
        this.path = path; this.methods = List.copyOf(methods); this.contentTypes = List.copyOf(contentTypes);
        this.headerLengthMin = headerLengthMin; this.headerLengthMax = headerLengthMax;
        this.bodyLengthMin = bodyLengthMin; this.bodyLengthMax = bodyLengthMax; this.upstreamId = upstreamId;
        this.proxyConnectTimeoutMs = proxyConnectTimeoutMs; this.proxyReadTimeoutMs = proxyReadTimeoutMs;
        this.proxySendTimeoutMs = proxySendTimeoutMs;
        this.rateLimitEnabled = rateLimitEnabled; this.ratePerSecond = ratePerSecond;
        this.rateLimitBurst = rateLimitBurst; this.rateLimitNodelay = rateLimitNodelay;
    }

    public UUID getId() { return id; }
    public UUID getServerId() { return serverId; }
    public String getPath() { return path; }
    public List<String> getMethods() { return methods; }
    public List<String> getContentTypes() { return contentTypes; }
    public int getHeaderLengthMin() { return headerLengthMin; }
    public int getHeaderLengthMax() { return headerLengthMax; }
    public long getBodyLengthMin() { return bodyLengthMin; }
    public long getBodyLengthMax() { return bodyLengthMax; }
    public UUID getUpstreamId() { return upstreamId; }
    public int getProxyConnectTimeoutMs() { return proxyConnectTimeoutMs; }
    public int getProxyReadTimeoutMs() { return proxyReadTimeoutMs; }
    public int getProxySendTimeoutMs() { return proxySendTimeoutMs; }
    public boolean isRateLimitEnabled() { return rateLimitEnabled; }
    public int getRatePerSecond() { return ratePerSecond; }
    public int getRateLimitBurst() { return rateLimitBurst; }
    public boolean isRateLimitNodelay() { return rateLimitNodelay; }
    public boolean isDynamicDnsEnabled() { return dynamicDnsEnabled; }
    public String getDynamicDnsHost() { return dynamicDnsHost; }
    public Integer getDynamicDnsPort() { return dynamicDnsPort; }
    public boolean isIpPolicyEnabled() { return ipPolicyEnabled; }
    public boolean isApiPolicyEnabled() { return apiPolicyEnabled; }
    public void applyDynamicDns(boolean enabled, String host, Integer port) { this.dynamicDnsEnabled=enabled; this.dynamicDnsHost=host; this.dynamicDnsPort=port; }
    public void applyPolicySettings(boolean ipPolicyEnabled, boolean apiPolicyEnabled) {
        this.ipPolicyEnabled = ipPolicyEnabled;
        this.apiPolicyEnabled = apiPolicyEnabled;
    }
    public void applyDirectives(LocationAction action, String rootPath, String aliasPath, Integer returnStatus, String returnBody, ReturnContentTypeMode typeMode, String contentType, List<String> headers) { this.action=action; this.rootPath=rootPath; this.aliasPath=aliasPath; this.returnStatus=returnStatus; this.returnBody=returnBody; this.returnContentTypeMode=typeMode; this.returnContentType=contentType; this.responseHeaders=List.copyOf(headers); }
    public LocationAction getAction(){return action;} public String getRootPath(){return rootPath;} public String getAliasPath(){return aliasPath;} public Integer getReturnStatus(){return returnStatus;} public String getReturnBody(){return returnBody;} public ReturnContentTypeMode getReturnContentTypeMode(){return returnContentTypeMode;} public String getReturnContentType(){return returnContentType;} public List<String> getResponseHeaders(){return responseHeaders;}
}
