package net.daoke.openrestyplus.httpconfig;

import jakarta.persistence.Column;
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
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected HttpLocation() { }

    public HttpLocation(UUID serverId, String path, List<String> methods, List<String> contentTypes,
                        int headerLengthMin, int headerLengthMax, long bodyLengthMin, long bodyLengthMax,
                        UUID upstreamId, int proxyConnectTimeoutMs, int proxyReadTimeoutMs, int proxySendTimeoutMs) {
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
    }
    public void apply(String path, List<String> methods, List<String> contentTypes,
                      int headerLengthMin, int headerLengthMax, long bodyLengthMin, long bodyLengthMax,
                      UUID upstreamId, int proxyConnectTimeoutMs, int proxyReadTimeoutMs, int proxySendTimeoutMs) {
        this.path = path; this.methods = List.copyOf(methods); this.contentTypes = List.copyOf(contentTypes);
        this.headerLengthMin = headerLengthMin; this.headerLengthMax = headerLengthMax;
        this.bodyLengthMin = bodyLengthMin; this.bodyLengthMax = bodyLengthMax; this.upstreamId = upstreamId;
        this.proxyConnectTimeoutMs = proxyConnectTimeoutMs; this.proxyReadTimeoutMs = proxyReadTimeoutMs;
        this.proxySendTimeoutMs = proxySendTimeoutMs;
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
}
