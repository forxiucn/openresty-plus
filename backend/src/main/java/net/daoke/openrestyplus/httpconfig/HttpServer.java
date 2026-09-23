package net.daoke.openrestyplus.httpconfig;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

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
    @Column(name = "upstream_id") @JdbcTypeCode(Types.BINARY) private UUID upstreamId;
    @Column(name = "access_log", nullable = false, length = 512) private String accessLog;
    @Column(name = "error_log", nullable = false, length = 512) private String errorLog;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected HttpServer() { }

    public HttpServer(UUID centerId, String domain, int listenPort, boolean sslEnabled, UUID upstreamId,
                      String accessLog, String errorLog) {
        this.centerId = centerId;
        this.domain = domain;
        this.listenPort = listenPort;
        this.sslEnabled = sslEnabled;
        this.upstreamId = upstreamId;
        this.accessLog = accessLog;
        this.errorLog = errorLog;
    }
    public void apply(String domain, int listenPort, boolean sslEnabled, UUID upstreamId, String accessLog, String errorLog) {
        this.domain = domain; this.listenPort = listenPort; this.sslEnabled = sslEnabled; this.upstreamId = upstreamId;
        this.accessLog = accessLog; this.errorLog = errorLog;
    }

    public UUID getId() { return id; }
    public UUID getCenterId() { return centerId; }
    public String getDomain() { return domain; }
    public int getListenPort() { return listenPort; }
    public boolean isSslEnabled() { return sslEnabled; }
    public UUID getUpstreamId() { return upstreamId; }
    public String getAccessLog() { return accessLog; }
    public String getErrorLog() { return errorLog; }
}
