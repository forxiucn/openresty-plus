package net.daoke.openrestyplus.reload;

import net.daoke.openrestyplus.audit.AuditService;
import net.daoke.openrestyplus.center.CenterRepository;
import net.daoke.openrestyplus.node.NginxNode;
import net.daoke.openrestyplus.node.NginxNodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Invokes the Nginx 1.31 Control API through a node's private HTTP endpoint.
 * PATCHing an empty config change set validates and reloads the active native configuration.
 */
@Service
public class ControlApiReloadService {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final CenterRepository centers;
    private final NginxNodeRepository nodes;
    private final ControlApiReloadTaskRepository tasks;
    private final ControlApiReloadNodeResultRepository results;
    private final AuditService audit;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

    public ControlApiReloadService(CenterRepository centers, NginxNodeRepository nodes,
                                   ControlApiReloadTaskRepository tasks,
                                   ControlApiReloadNodeResultRepository results,
                                   AuditService audit) {
        this.centers = centers;
        this.nodes = nodes;
        this.tasks = tasks;
        this.results = results;
        this.audit = audit;
    }

    public ReloadTaskView reloadCenter(UUID centerId) {
        requireCenter(centerId);
        var task = tasks.save(new ControlApiReloadTask(centerId));
        var enabledNodes = nodes.findByCenterIdOrderByName(centerId).stream().filter(NginxNode::isEnabled).toList();

        if (enabledNodes.isEmpty()) {
            task.complete("FAILED");
            tasks.save(task);
            audit.failure(centerId, "CONTROL_API_RELOAD", "CONTROL_API_RELOAD_TASK", task.getId());
            return toView(task, List.of());
        }

        List<ControlApiReloadNodeResult> nodeResults;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            nodeResults = executor.invokeAll(enabledNodes.stream()
                    .<java.util.concurrent.Callable<ControlApiReloadNodeResult>>map(node -> () -> reloadNode(task.getId(), node))
                    .toList())
                .stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception exception) {
                        throw new IllegalStateException("Reload worker did not return a result", exception);
                    }
                })
                .toList();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            nodeResults = List.of(new ControlApiReloadNodeResult(task.getId(), UUID.randomUUID(), "编排线程", "FAILED", null,
                "重载编排被中断"));
        }

        results.saveAll(nodeResults);
        var successCount = nodeResults.stream().filter(result -> "SUCCESS".equals(result.getStatus())).count();
        task.complete(successCount == nodeResults.size() ? "SUCCESS" : successCount == 0 ? "FAILED" : "PARTIAL_SUCCESS");
        tasks.save(task);
        if ("SUCCESS".equals(task.getStatus())) {
            audit.success(centerId, "CONTROL_API_RELOAD", "CONTROL_API_RELOAD_TASK", task.getId());
        } else {
            audit.failure(centerId, "CONTROL_API_RELOAD", "CONTROL_API_RELOAD_TASK", task.getId());
        }
        return toView(task, nodeResults);
    }

    public List<ReloadTaskView> list(UUID centerId) {
        requireCenter(centerId);
        return tasks.findByCenterIdOrderByCreatedAtDesc(centerId).stream()
            .map(task -> toView(task, results.findByTaskIdOrderByCompletedAtAsc(task.getId())))
            .toList();
    }

    public ReloadTaskView get(UUID centerId, UUID taskId) {
        requireCenter(centerId);
        var task = tasks.findById(taskId).filter(value -> value.getCenterId().equals(centerId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reload task not found"));
        return toView(task, results.findByTaskIdOrderByCompletedAtAsc(taskId));
    }

    private ControlApiReloadNodeResult reloadNode(UUID taskId, NginxNode node) {
        try {
            var target = controlConfigUri(node.getControlApiUrl());
            var request = HttpRequest.newBuilder(target)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("[]"))
                .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            var status = response.statusCode() >= 200 && response.statusCode() < 300 ? "SUCCESS" : "FAILED";
            return new ControlApiReloadNodeResult(taskId, node.getId(), node.getName(), status, response.statusCode(),
                summarize(response.body()));
        } catch (Exception exception) {
            return new ControlApiReloadNodeResult(taskId, node.getId(), node.getName(), "FAILED", null,
                summarize(exception.getMessage()));
        }
    }

    private URI controlConfigUri(String configuredUrl) {
        if (configuredUrl == null || configuredUrl.isBlank()) {
            throw new IllegalArgumentException("未配置 Control API 地址");
        }
        var source = URI.create(configuredUrl.trim());
        if ((!("http".equalsIgnoreCase(source.getScheme()) || "https".equalsIgnoreCase(source.getScheme())))
            || source.getHost() == null || source.getUserInfo() != null) {
            throw new IllegalArgumentException("Control API 地址必须是无凭据的 HTTP(S) 地址");
        }
        var path = source.getPath() == null ? "" : source.getPath().replaceAll("/+$", "");
        if (!path.endsWith("/1/control/config")) {
            path = path + "/1/control/config";
        }
        return URI.create(source.getScheme() + "://" + source.getAuthority() + path);
    }

    private String summarize(String text) {
        if (text == null || text.isBlank()) return null;
        var normalized = text.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= 1024 ? normalized : normalized.substring(0, 1024);
    }

    private void requireCenter(UUID centerId) {
        if (!centers.existsById(centerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found");
        }
    }

    private ReloadTaskView toView(ControlApiReloadTask task, List<ControlApiReloadNodeResult> nodeResults) {
        return new ReloadTaskView(task.getId(), task.getCenterId(), task.getStatus(), task.getCreatedAt(), task.getCompletedAt(),
            nodeResults.stream().map(result -> new NodeResultView(result.getNodeId(), result.getNodeName(), result.getStatus(),
                result.getHttpStatus(), result.getMessage(), result.getCompletedAt())).toList());
    }

    public record ReloadTaskView(UUID id, UUID centerId, String status, java.time.Instant createdAt,
                                 java.time.Instant completedAt, List<NodeResultView> nodeResults) { }
    public record NodeResultView(UUID nodeId, String nodeName, String status, Integer httpStatus,
                                 String message, java.time.Instant completedAt) { }
}
