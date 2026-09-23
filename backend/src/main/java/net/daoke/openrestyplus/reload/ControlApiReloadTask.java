package net.daoke.openrestyplus.reload;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_api_reload_task")
public class ControlApiReloadTask {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY)
    private UUID id;
    @Column(name = "center_id", nullable = false) @JdbcTypeCode(Types.BINARY)
    private UUID centerId;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "completed_at")
    private Instant completedAt;

    protected ControlApiReloadTask() { }

    public ControlApiReloadTask(UUID centerId) {
        this.centerId = centerId;
        this.status = "RUNNING";
    }

    public void complete(String status) {
        this.status = status;
        this.completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCenterId() { return centerId; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
