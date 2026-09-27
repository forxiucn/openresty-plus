package net.daoke.openrestyplus.node;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.daoke.openrestyplus.center.CenterRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@RestController
@RequestMapping("/api/centers/{centerId}/nodes/{nodeId}/logs")
@Tag(name = "OpenResty log stream")
public class NginxNodeLogStreamController {
    private final CenterRepository centers;
    private final NginxNodeRepository nodes;
    private final OpenrestyLogStream stream;

    public NginxNodeLogStreamController(CenterRepository centers, NginxNodeRepository nodes, OpenrestyLogStream stream) {
        this.centers = centers; this.nodes = nodes; this.stream = stream;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream Kafka logs from an OpenResty node")
    public SseEmitter stream(@PathVariable UUID centerId, @PathVariable UUID nodeId) {
        if (!centers.existsById(centerId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found");
        var node = nodes.findById(nodeId).filter(value -> value.getCenterId().equals(centerId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nginx node not found"));
        return stream.subscribe(stream.resolveKey(nodeId.toString(), node.getName()));
    }
}
