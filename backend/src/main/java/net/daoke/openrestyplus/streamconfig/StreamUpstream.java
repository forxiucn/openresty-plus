package net.daoke.openrestyplus.streamconfig;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stream_upstream", uniqueConstraints = @UniqueConstraint(name = "uk_stream_upstream_center_name", columnNames = {"center_id", "name"}))
public class StreamUpstream {
  @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY) private UUID id;
  @Column(name = "center_id", nullable = false) @JdbcTypeCode(Types.BINARY) private UUID centerId;
  @Column(nullable = false, length = 128) private String name;
  @Column(name = "target_host", nullable = false, length = 255) private String targetHost;
  @Column(name = "target_port", nullable = false) private int targetPort;
  @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
  protected StreamUpstream() { }
  public StreamUpstream(UUID centerId, String name, String targetHost, int targetPort) { this.centerId=centerId; apply(name,targetHost,targetPort); }
  public void apply(String name, String targetHost, int targetPort) { this.name=name; this.targetHost=targetHost; this.targetPort=targetPort; }
  public UUID getId(){return id;} public UUID getCenterId(){return centerId;} public String getName(){return name;} public String getTargetHost(){return targetHost;} public int getTargetPort(){return targetPort;}
}
