package net.daoke.openrestyplus.node;

import net.daoke.openrestyplus.center.CenterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the built-in Nginx stub_status endpoint from each enabled HTTP node. */
@RestController
@RequestMapping("/api/centers/{centerId}/node-metrics")
public class NginxNodeMetricsController {
    private static final Pattern ACTIVE = Pattern.compile("Active connections:\\s*(\\d+)");
    private static final Pattern COUNTERS = Pattern.compile("\\s(\\d+)\\s+(\\d+)\\s+(\\d+)\\s*");
    private static final Pattern STATES = Pattern.compile("Reading:\\s*(\\d+)\\s+Writing:\\s*(\\d+)\\s+Waiting:\\s*(\\d+)");
    private final CenterRepository centers;
    private final NginxNodeRepository nodes;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public NginxNodeMetricsController(CenterRepository centers, NginxNodeRepository nodes) { this.centers = centers; this.nodes = nodes; }

    @GetMapping
    public List<NodeMetricsView> list(@PathVariable UUID centerId) {
        if (!centers.existsById(centerId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found");
        return nodes.findByCenterIdOrderByName(centerId).stream().map(this::read).toList();
    }

    private NodeMetricsView read(NginxNode node) {
        if (!node.isEnabled()) return NodeMetricsView.unavailable(node, "该节点未启用");
        try {
            var ports = new java.util.ArrayList<Integer>();
            ports.add(node.getServicePort());
            HttpResponse<String> response = null;
            for (var candidate : new java.util.LinkedHashSet<>(ports)) {
                try {
                    var request = HttpRequest.newBuilder(URI.create("http://" + node.getHost() + ":" + candidate + "/__openresty_plus/status"))
                        .timeout(Duration.ofSeconds(3)).GET().build();
                    var candidateResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (candidateResponse.statusCode() == 200) { response = candidateResponse; break; }
                } catch (java.io.IOException ignored) { }
            }
            if (response == null) return NodeMetricsView.unavailable(node, "无法连接节点状态接口");
            if (response.statusCode() != 200) return NodeMetricsView.unavailable(node, "状态接口返回 HTTP " + response.statusCode());
            Matcher active = ACTIVE.matcher(response.body()), counters = COUNTERS.matcher(response.body()), states = STATES.matcher(response.body());
            if (!active.find() || !counters.find() || !states.find()) return NodeMetricsView.unavailable(node, "状态接口返回格式无效");
            return new NodeMetricsView(node.getId(), node.getName(), node.getHost(), node.getServicePort(), true, null,
                Long.parseLong(active.group(1)), Long.parseLong(counters.group(1)), Long.parseLong(counters.group(2)), Long.parseLong(counters.group(3)),
                Long.parseLong(states.group(1)), Long.parseLong(states.group(2)), Long.parseLong(states.group(3)));
        } catch (Exception exception) { return NodeMetricsView.unavailable(node, "无法读取节点状态接口：" + exception.getClass().getSimpleName()); }
    }

    public record NodeMetricsView(UUID nodeId, String nodeName, String host, int port, boolean available, String message,
                                  Long activeConnections, Long accepts, Long handled, Long requests, Long reading, Long writing, Long waiting) {
        static NodeMetricsView unavailable(NginxNode node, String message) { return new NodeMetricsView(node.getId(), node.getName(), node.getHost(), node.getServicePort(), false, message, null, null, null, null, null, null, null); }
    }
}
