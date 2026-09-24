package net.daoke.openrestyplus.streamconfig;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stream_server", uniqueConstraints = @UniqueConstraint(name = "uk_stream_server_center_port_protocol", columnNames = {"center_id", "listen_port", "protocol"}))
public class StreamServer {
  @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY) private UUID id;
  @Column(name = "center_id", nullable = false) @JdbcTypeCode(Types.BINARY) private UUID centerId;
  @Column(name = "service_name", nullable = false, length = 128) private String serviceName;
  @Column(name = "listen_port", nullable = false) private int listenPort;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private StreamProtocol protocol;
  @Column(name = "upstream_id", nullable = false) @JdbcTypeCode(Types.BINARY) private UUID upstreamId;
  @Column(name = "access_log", nullable = false, length = 512) private String accessLog;
  @Column(name = "error_log", nullable = false, length = 512) private String errorLog;
  @Column(name="dynamic_dns_enabled",nullable=false) private boolean dynamicDnsEnabled;
  @Column(name="dynamic_dns_host") private String dynamicDnsHost;
  @Column(name="dynamic_dns_port") private Integer dynamicDnsPort;
  @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
  protected StreamServer() { }
  public StreamServer(UUID centerId, String serviceName, int listenPort, StreamProtocol protocol, UUID upstreamId, String accessLog, String errorLog) { this.centerId=centerId; apply(serviceName,listenPort,protocol,upstreamId,accessLog,errorLog); }
  public void apply(String serviceName, int listenPort, StreamProtocol protocol, UUID upstreamId, String accessLog, String errorLog) { this.serviceName=serviceName; this.listenPort=listenPort; this.protocol=protocol; this.upstreamId=upstreamId; this.accessLog=accessLog; this.errorLog=errorLog; }
  public UUID getId(){return id;} public UUID getCenterId(){return centerId;} public String getServiceName(){return serviceName;} public int getListenPort(){return listenPort;} public StreamProtocol getProtocol(){return protocol;} public UUID getUpstreamId(){return upstreamId;} public String getAccessLog(){return accessLog;} public String getErrorLog(){return errorLog;}
  public boolean isDynamicDnsEnabled(){return dynamicDnsEnabled;} public String getDynamicDnsHost(){return dynamicDnsHost;} public Integer getDynamicDnsPort(){return dynamicDnsPort;} public void applyDynamicDns(boolean enabled,String host,Integer port){this.dynamicDnsEnabled=enabled;this.dynamicDnsHost=host;this.dynamicDnsPort=port;}
}
