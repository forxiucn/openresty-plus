package net.daoke.openrestyplus.node;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.daoke.openrestyplus.center.CenterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/centers/{centerId}/nodes/{nodeId}/logs")
@Tag(name = "OpenResty logs")
public class NginxNodeLogController {
    private final CenterRepository centers;
    private final NginxNodeRepository nodes;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public NginxNodeLogController(CenterRepository centers, NginxNodeRepository nodes) {
        this.centers = centers;
        this.nodes = nodes;
    }

    @GetMapping
    @Operation(summary = "Read the latest log lines from an OpenResty node")
    public String read(@PathVariable UUID centerId, @PathVariable UUID nodeId,
                       @RequestParam String path, @RequestParam(defaultValue = "200") int lines) {
        if (!centers.existsById(centerId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found");
        var node = nodes.findById(nodeId).filter(value -> value.getCenterId().equals(centerId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nginx node not found"));
        if (path == null || !path.startsWith("/var/log/nginx/") || path.contains("..") || path.indexOf('\n') >= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "日志路径必须位于 /var/log/nginx/ 下");
        var count = Math.min(Math.max(lines, 1), 1000);
        try {
            var base = "http://" + node.getHost() + ":" + node.getServicePort() + "/runtime/logs";
            var uri = URI.create(base + "?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8) + "&lines=" + count);
            var response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "节点日志读取失败：" + response.body());
            return response.body();
        } catch (ResponseStatusException exception) { throw exception; }
        catch (Exception exception) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法连接 OpenResty 节点日志接口", exception); }
    }
}
