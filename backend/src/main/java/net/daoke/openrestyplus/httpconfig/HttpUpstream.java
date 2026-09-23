package net.daoke.openrestyplus.httpconfig;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "http_upstream")
public class HttpUpstream {
  @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY) private UUID id;
  @Column(name="center_id", nullable=false) @JdbcTypeCode(Types.BINARY) private UUID centerId;
  @Column(nullable=false) private String name;
  @Column(name="keepalive_connections", nullable=false) private int keepaliveConnections;
  @Column(name="health_check_enabled", nullable=false) private boolean healthCheckEnabled;
  @Column(name="health_check_path", nullable=false) private String healthCheckPath="/health";
  @Column(name="health_check_interval_seconds", nullable=false) private int healthCheckIntervalSeconds=10;
  @Column(name="health_check_timeout_milliseconds", nullable=false) private int healthCheckTimeoutMilliseconds=1000;
  @Column(name="health_check_expected_status", nullable=false) private int healthCheckExpectedStatus=200;
  @Column(name="created_at", nullable=false) private Instant createdAt=Instant.now();
  protected HttpUpstream() {}
  public HttpUpstream(UUID centerId,String name,int keepaliveConnections,boolean healthCheckEnabled,String healthCheckPath,int healthCheckIntervalSeconds,int healthCheckTimeoutMilliseconds,int healthCheckExpectedStatus){this.centerId=centerId;apply(name,keepaliveConnections,healthCheckEnabled,healthCheckPath,healthCheckIntervalSeconds,healthCheckTimeoutMilliseconds,healthCheckExpectedStatus);}
  public void apply(String name,int keepaliveConnections,boolean healthCheckEnabled,String healthCheckPath,int healthCheckIntervalSeconds,int healthCheckTimeoutMilliseconds,int healthCheckExpectedStatus){this.name=name;this.keepaliveConnections=keepaliveConnections;this.healthCheckEnabled=healthCheckEnabled;this.healthCheckPath=healthCheckPath;this.healthCheckIntervalSeconds=healthCheckIntervalSeconds;this.healthCheckTimeoutMilliseconds=healthCheckTimeoutMilliseconds;this.healthCheckExpectedStatus=healthCheckExpectedStatus;}
  public UUID getId(){return id;} public String getName(){return name;} public int getKeepaliveConnections(){return keepaliveConnections;} public boolean isHealthCheckEnabled(){return healthCheckEnabled;} public String getHealthCheckPath(){return healthCheckPath;} public int getHealthCheckIntervalSeconds(){return healthCheckIntervalSeconds;} public int getHealthCheckTimeoutMilliseconds(){return healthCheckTimeoutMilliseconds;} public int getHealthCheckExpectedStatus(){return healthCheckExpectedStatus;}
}
