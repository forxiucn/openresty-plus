package net.daoke.openrestyplus.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "runtime_configuration_version")
public class RuntimeConfigurationVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY)
    private UUID id;
    @Column(name = "center_id", nullable = false) @JdbcTypeCode(Types.BINARY)
    private UUID centerId;
    @Column(name = "version_no", nullable = false)
    private long versionNo;
    @Column(nullable = false, length = 64)
    private String checksum;
    @Column(nullable = false, length = 16)
    private String state;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "json")
    private JsonNode content;
    @Column(name = "created_by", nullable = false, length = 128)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected RuntimeConfigurationVersion() { }

    public RuntimeConfigurationVersion(UUID centerId, long versionNo, String checksum, JsonNode content, String createdBy) {
        this(centerId, versionNo, checksum, content, createdBy, "PUBLISHED");
    }

    public RuntimeConfigurationVersion(UUID centerId, long versionNo, String checksum, JsonNode content,
                                       String createdBy, String state) {
        this.centerId = centerId;
        this.versionNo = versionNo;
        this.checksum = checksum;
        this.content = content;
        this.createdBy = createdBy;
        this.state = state;
    }

    public UUID getId() { return id; }
    public UUID getCenterId() { return centerId; }
    public long getVersionNo() { return versionNo; }
    public String getChecksum() { return checksum; }
    public String getState() { return state; }
    public JsonNode getContent() { return content; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
