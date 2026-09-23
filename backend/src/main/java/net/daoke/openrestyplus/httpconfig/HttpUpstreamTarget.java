package net.daoke.openrestyplus.httpconfig;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.util.UUID;

@Entity
@Table(name = "http_upstream_target")
public class HttpUpstreamTarget {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY) private UUID id;
    @Column(name = "upstream_id", nullable = false) @JdbcTypeCode(Types.BINARY) private UUID upstreamId;
    @Column(name = "target_host", nullable = false) private String targetHost;
    @Column(name = "target_port", nullable = false) private int targetPort;
    @Column(nullable = false) private int weight;
    @Column(name = "max_fails", nullable = false) private int maxFails;
    @Column(name = "fail_timeout_seconds", nullable = false) private int failTimeoutSeconds;
    @Column(nullable = false) private boolean backup;
    @Column(nullable = false) private boolean enabled;
    protected HttpUpstreamTarget() { }
    public HttpUpstreamTarget(UUID upstreamId, String targetHost, int targetPort, int weight, int maxFails, int failTimeoutSeconds, boolean backup, boolean enabled) { this.upstreamId=upstreamId; apply(targetHost,targetPort,weight,maxFails,failTimeoutSeconds,backup,enabled); }
    public void apply(String host,int port,int weight,int maxFails,int timeout,boolean backup,boolean enabled) { this.targetHost=host;this.targetPort=port;this.weight=weight;this.maxFails=maxFails;this.failTimeoutSeconds=timeout;this.backup=backup;this.enabled=enabled; }
    public UUID getId(){return id;} public UUID getUpstreamId(){return upstreamId;} public String getTargetHost(){return targetHost;} public int getTargetPort(){return targetPort;} public int getWeight(){return weight;} public int getMaxFails(){return maxFails;} public int getFailTimeoutSeconds(){return failTimeoutSeconds;} public boolean isBackup(){return backup;} public boolean isEnabled(){return enabled;}
}
