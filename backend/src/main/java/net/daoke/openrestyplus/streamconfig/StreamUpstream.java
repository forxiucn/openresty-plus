package net.daoke.openrestyplus.streamconfig;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import net.daoke.openrestyplus.health.HealthCheckType;

@Entity
@Table(name = "stream_upstream", uniqueConstraints = @UniqueConstraint(name = "uk_stream_upstream_center_name", columnNames = {"center_id", "name"}))
public class StreamUpstream {
  @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY) private UUID id;
  @Column(name = "center_id", nullable = false) @JdbcTypeCode(Types.BINARY) private UUID centerId;
  @Column(nullable = false, length = 128) private String name;
  @Column(name = "target_host", nullable = false, length = 255) private String targetHost;
  @Column(name = "target_port", nullable = false) private int targetPort;
  @Column(name = "resolve_enabled", nullable = false) private boolean resolveEnabled;
  @Column(name = "zone_size_kilobytes", nullable = false) private int zoneSizeKilobytes=64;
  @Column(name="health_check_enabled", nullable=false) private boolean healthCheckEnabled;
  @Enumerated(EnumType.STRING) @Column(name="health_check_type", nullable=false) private HealthCheckType healthCheckType=HealthCheckType.TCP;
  @Column(name="health_check_path", nullable=false) private String healthCheckPath="/health";
  @Column(name="health_check_host") private String healthCheckHost;
  @Column(name="health_check_interval_seconds", nullable=false) private int healthCheckIntervalSeconds=10;
  @Column(name="health_check_timeout_milliseconds", nullable=false) private int healthCheckTimeoutMilliseconds=1000;
  @Column(name="health_check_expected_status", nullable=false) private int healthCheckExpectedStatus=200;
  @JdbcTypeCode(SqlTypes.JSON) @Column(name="health_check_request_headers", nullable=false) private List<String> healthCheckRequestHeaders=List.of();
  @Column(name="health_check_rise", nullable=false) private int healthCheckRise=2;
  @Column(name="health_check_fall", nullable=false) private int healthCheckFall=3;
  @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
  protected StreamUpstream() { }
  public StreamUpstream(UUID centerId,String name,String targetHost,int targetPort,boolean resolveEnabled,int zoneSizeKilobytes,boolean healthCheckEnabled,HealthCheckType healthCheckType,String healthCheckPath,String healthCheckHost,int healthCheckIntervalSeconds,int healthCheckTimeoutMilliseconds,int healthCheckExpectedStatus,List<String> healthCheckRequestHeaders,int healthCheckRise,int healthCheckFall){this.centerId=centerId;apply(name,targetHost,targetPort,resolveEnabled,zoneSizeKilobytes,healthCheckEnabled,healthCheckType,healthCheckPath,healthCheckHost,healthCheckIntervalSeconds,healthCheckTimeoutMilliseconds,healthCheckExpectedStatus,healthCheckRequestHeaders,healthCheckRise,healthCheckFall);}
  public void apply(String name,String targetHost,int targetPort,boolean resolveEnabled,int zoneSizeKilobytes,boolean healthCheckEnabled,HealthCheckType healthCheckType,String healthCheckPath,String healthCheckHost,int healthCheckIntervalSeconds,int healthCheckTimeoutMilliseconds,int healthCheckExpectedStatus,List<String> healthCheckRequestHeaders,int healthCheckRise,int healthCheckFall){this.name=name;this.targetHost=targetHost;this.targetPort=targetPort;this.resolveEnabled=resolveEnabled;this.zoneSizeKilobytes=zoneSizeKilobytes;this.healthCheckEnabled=healthCheckEnabled;this.healthCheckType=healthCheckType;this.healthCheckPath=healthCheckPath;this.healthCheckHost=healthCheckHost;this.healthCheckIntervalSeconds=healthCheckIntervalSeconds;this.healthCheckTimeoutMilliseconds=healthCheckTimeoutMilliseconds;this.healthCheckExpectedStatus=healthCheckExpectedStatus;this.healthCheckRequestHeaders=healthCheckRequestHeaders==null?List.of():List.copyOf(healthCheckRequestHeaders);this.healthCheckRise=healthCheckRise;this.healthCheckFall=healthCheckFall;}
  public UUID getId(){return id;} public UUID getCenterId(){return centerId;} public String getName(){return name;} public String getTargetHost(){return targetHost;} public int getTargetPort(){return targetPort;} public boolean isResolveEnabled(){return resolveEnabled;} public int getZoneSizeKilobytes(){return zoneSizeKilobytes;} public boolean isHealthCheckEnabled(){return healthCheckEnabled;} public HealthCheckType getHealthCheckType(){return healthCheckType;} public String getHealthCheckPath(){return healthCheckPath;} public String getHealthCheckHost(){return healthCheckHost;} public int getHealthCheckIntervalSeconds(){return healthCheckIntervalSeconds;} public int getHealthCheckTimeoutMilliseconds(){return healthCheckTimeoutMilliseconds;} public int getHealthCheckExpectedStatus(){return healthCheckExpectedStatus;} public List<String> getHealthCheckRequestHeaders(){return healthCheckRequestHeaders;} public int getHealthCheckRise(){return healthCheckRise;} public int getHealthCheckFall(){return healthCheckFall;}
}
