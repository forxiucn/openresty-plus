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
  @Column(name="created_at", nullable=false) private Instant createdAt=Instant.now();
  protected HttpUpstream() {}
  public HttpUpstream(UUID centerId,String name,int keepaliveConnections){this.centerId=centerId;this.name=name;this.keepaliveConnections=keepaliveConnections;}
  public void apply(String name, int keepaliveConnections){this.name=name;this.keepaliveConnections=keepaliveConnections;}
  public UUID getId(){return id;} public String getName(){return name;} public int getKeepaliveConnections(){return keepaliveConnections;}
}
