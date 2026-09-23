package net.daoke.openrestyplus.center;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "center", uniqueConstraints = @UniqueConstraint(name = "uk_center_code", columnNames = "code"))
public class Center {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    private UUID id;
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 128) private String name;
    @Column(nullable = false) private boolean enabled = true;
    @Column(nullable = false) private Instant createdAt = Instant.now();
    protected Center() {}
    public Center(String code, String name) { this.code = code; this.name = name; }
    public void apply(String code, String name, boolean enabled) { this.code = code; this.name = name; this.enabled = enabled; }
    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
}
