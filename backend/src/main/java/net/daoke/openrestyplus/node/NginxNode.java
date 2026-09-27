package net.daoke.openrestyplus.node;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nginx_node", uniqueConstraints = @UniqueConstraint(name = "uk_nginx_node_center_name", columnNames = {"center_id", "name"}))
public class NginxNode {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY)
    private UUID id;
    @Column(name = "center_id", nullable = false) @JdbcTypeCode(Types.BINARY)
    private UUID centerId;
    @Column(nullable = false, length = 128) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = true, length = 32) private NginxProtocol protocol;
    @Column(nullable = false, length = 255) private String host;
    @Column(name = "service_port", nullable = false) private int servicePort;
    @Column(name = "control_api_url", length = 512) private String controlApiUrl;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected NginxNode() { }

    public NginxNode(UUID centerId, String name, String host, int servicePort, String controlApiUrl) {
        this.centerId = centerId;
        this.name = name;
        this.protocol = null;
        this.host = host;
        this.servicePort = servicePort;
        this.controlApiUrl = controlApiUrl;
    }
    public void apply(String name, String host, int servicePort, String controlApiUrl, boolean enabled) {
        this.name = name; this.host = host; this.servicePort = servicePort;
        this.controlApiUrl = controlApiUrl; this.enabled = enabled;
    }

    public UUID getId() { return id; }
    public UUID getCenterId() { return centerId; }
    public String getName() { return name; }
    public NginxProtocol getProtocol() { return protocol; }
    public String getHost() { return host; }
    public int getServicePort() { return servicePort; }
    public String getControlApiUrl() { return controlApiUrl; }
    public boolean isEnabled() { return enabled; }
}
