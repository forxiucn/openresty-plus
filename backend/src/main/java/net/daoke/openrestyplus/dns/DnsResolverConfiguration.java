package net.daoke.openrestyplus.dns;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "dns_resolver_configuration")
public class DnsResolverConfiguration {
  @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY) private UUID id;
  @Column(name="center_id",nullable=false) @JdbcTypeCode(Types.BINARY) private UUID centerId;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private DnsResolverScope scope;
  @Column(name="target_resource_id") @JdbcTypeCode(Types.BINARY) private UUID targetResourceId;
  @JdbcTypeCode(SqlTypes.JSON) @Column(name="resolver_addresses",nullable=false,columnDefinition="json") private List<String> resolverAddresses;
  @Column(name="valid_seconds",nullable=false) private int validSeconds;
  @Column(name="timeout_milliseconds",nullable=false) private int timeoutMilliseconds;
  @Column(name="ipv6_enabled",nullable=false) private boolean ipv6Enabled;
  @Column(nullable=false) private boolean enabled;
  protected DnsResolverConfiguration(){}
  public DnsResolverConfiguration(UUID centerId,DnsResolverScope scope,UUID targetResourceId,List<String> addresses,int valid,int timeout,boolean ipv6,boolean enabled){this.centerId=centerId;apply(scope,targetResourceId,addresses,valid,timeout,ipv6,enabled);}
  public void apply(DnsResolverScope scope,UUID targetResourceId,List<String> addresses,int valid,int timeout,boolean ipv6,boolean enabled){this.scope=scope;this.targetResourceId=targetResourceId;this.resolverAddresses=List.copyOf(addresses);this.validSeconds=valid;this.timeoutMilliseconds=timeout;this.ipv6Enabled=ipv6;this.enabled=enabled;}
  public UUID getId(){return id;} public UUID getCenterId(){return centerId;} public DnsResolverScope getScope(){return scope;} public UUID getTargetResourceId(){return targetResourceId;} public List<String> getResolverAddresses(){return resolverAddresses;} public int getValidSeconds(){return validSeconds;} public int getTimeoutMilliseconds(){return timeoutMilliseconds;} public boolean isIpv6Enabled(){return ipv6Enabled;} public boolean isEnabled(){return enabled;}
}
