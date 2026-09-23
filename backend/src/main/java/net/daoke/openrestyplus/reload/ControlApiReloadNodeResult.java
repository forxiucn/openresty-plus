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
@Table(name = "control_api_reload_node_result")
public class ControlApiReloadNodeResult {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY)
    private UUID id;
    @Column(name = "task_id", nullable = false) @JdbcTypeCode(Types.BINARY)
    private UUID taskId;
    @Column(name = "node_id", nullable = false) @JdbcTypeCode(Types.BINARY)
    private UUID nodeId;
    @Column(name = "node_name", nullable = false, length = 128)
    private String nodeName;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "http_status")
    private Integer httpStatus;
    @Column(length = 1024)
    private String message;
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt = Instant.now();

    protected ControlApiReloadNodeResult() { }

    public ControlApiReloadNodeResult(UUID taskId, UUID nodeId, String nodeName, String status, Integer httpStatus, String message) {
        this.taskId = taskId;
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.status = status;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public UUID getId() { return id; }
    public UUID getTaskId() { return taskId; }
    public UUID getNodeId() { return nodeId; }
    public String getNodeName() { return nodeName; }
    public String getStatus() { return status; }
    public Integer getHttpStatus() { return httpStatus; }
    public String getMessage() { return message; }
    public Instant getCompletedAt() { return completedAt; }
}
