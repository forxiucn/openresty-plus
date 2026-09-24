package net.daoke.openrestyplus.httpconfig;

import jakarta.persistence.*;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import net.daoke.openrestyplus.health.HealthCheckType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "http_upstream")
public class HttpUpstream {
  @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY) private UUID id;
  @Column(name="center_id", nullable=false) @JdbcTypeCode(Types.BINARY) private UUID centerId;
  @Column(nullable=false) private String name;
  @Column(name="keepalive_connections", nullable=false) private int keepaliveConnections;
  @Column(name="zone_size_kilobytes", nullable=false) private int zoneSizeKilobytes=64;
  @Column(name="health_check_enabled", nullable=false) private boolean healthCheckEnabled;
  @Enumerated(EnumType.STRING) @Column(name="health_check_type", nullable=false) private HealthCheckType healthCheckType=HealthCheckType.HTTP;
  @Column(name="health_check_path", nullable=false) private String healthCheckPath="/health";
  @Column(name="health_check_interval_seconds", nullable=false) private int healthCheckIntervalSeconds=10;
  @Column(name="health_check_timeout_milliseconds", nullable=false) private int healthCheckTimeoutMilliseconds=1000;
  @Column(name="health_check_expected_status", nullable=false) private int healthCheckExpectedStatus=200;
  @Column(name="health_check_host") private String healthCheckHost;
  @JdbcTypeCode(SqlTypes.JSON) @Column(name="health_check_request_headers", nullable=false) private List<String> healthCheckRequestHeaders=List.of();
  @Column(name="health_check_rise", nullable=false) private int healthCheckRise=2;
  @Column(name="health_check_fall", nullable=false) private int healthCheckFall=3;
  @Column(name="created_at", nullable=false) private Instant createdAt=Instant.now();
  protected HttpUpstream() {}
  public HttpUpstream(UUID centerId,String name,int keepaliveConnections,int zoneSizeKilobytes,boolean healthCheckEnabled,HealthCheckType healthCheckType,String healthCheckPath,int healthCheckIntervalSeconds,int healthCheckTimeoutMilliseconds,int healthCheckExpectedStatus,String healthCheckHost,List<String> healthCheckRequestHeaders,int healthCheckRise,int healthCheckFall){this.centerId=centerId;apply(name,keepaliveConnections,zoneSizeKilobytes,healthCheckEnabled,healthCheckType,healthCheckPath,healthCheckIntervalSeconds,healthCheckTimeoutMilliseconds,healthCheckExpectedStatus,healthCheckHost,healthCheckRequestHeaders,healthCheckRise,healthCheckFall);}
  public void apply(String name,int keepaliveConnections,int zoneSizeKilobytes,boolean healthCheckEnabled,HealthCheckType healthCheckType,String healthCheckPath,int healthCheckIntervalSeconds,int healthCheckTimeoutMilliseconds,int healthCheckExpectedStatus,String healthCheckHost,List<String> healthCheckRequestHeaders,int healthCheckRise,int healthCheckFall){this.name=name;this.keepaliveConnections=keepaliveConnections;this.zoneSizeKilobytes=zoneSizeKilobytes;this.healthCheckEnabled=healthCheckEnabled;this.healthCheckType=healthCheckType;this.healthCheckPath=healthCheckPath;this.healthCheckIntervalSeconds=healthCheckIntervalSeconds;this.healthCheckTimeoutMilliseconds=healthCheckTimeoutMilliseconds;this.healthCheckExpectedStatus=healthCheckExpectedStatus;this.healthCheckHost=healthCheckHost;this.healthCheckRequestHeaders=healthCheckRequestHeaders==null?List.of():List.copyOf(healthCheckRequestHeaders);this.healthCheckRise=healthCheckRise;this.healthCheckFall=healthCheckFall;}
  public UUID getId(){return id;} public String getName(){return name;} public int getKeepaliveConnections(){return keepaliveConnections;} public int getZoneSizeKilobytes(){return zoneSizeKilobytes;} public boolean isHealthCheckEnabled(){return healthCheckEnabled;} public String getHealthCheckPath(){return healthCheckPath;} public int getHealthCheckIntervalSeconds(){return healthCheckIntervalSeconds;} public int getHealthCheckTimeoutMilliseconds(){return healthCheckTimeoutMilliseconds;} public int getHealthCheckExpectedStatus(){return healthCheckExpectedStatus;}
  public HealthCheckType getHealthCheckType(){return healthCheckType;} public String getHealthCheckHost(){return healthCheckHost;} public List<String> getHealthCheckRequestHeaders(){return healthCheckRequestHeaders;} public int getHealthCheckRise(){return healthCheckRise;} public int getHealthCheckFall(){return healthCheckFall;}
}
