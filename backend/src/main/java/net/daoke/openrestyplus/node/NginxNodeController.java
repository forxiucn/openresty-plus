package net.daoke.openrestyplus.node;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.daoke.openrestyplus.center.CenterRepository;
import net.daoke.openrestyplus.audit.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/centers/{centerId}/nodes")
@Tag(name = "Nginx nodes")
public class NginxNodeController {
    private final CenterRepository centers;
    private final NginxNodeRepository nodes;
    private final AuditService audit;

    public NginxNodeController(CenterRepository centers, NginxNodeRepository nodes, AuditService audit) {
        this.centers = centers;
        this.nodes = nodes;
        this.audit = audit;
    }

    @GetMapping
    @Operation(summary = "List Nginx nodes in a center")
    public List<NodeView> list(@PathVariable UUID centerId) {
        requireCenter(centerId);
        return nodes.findByCenterIdOrderByName(centerId).stream().map(NodeView::from).toList();
    }

    @GetMapping("/paged")
    public net.daoke.openrestyplus.web.PageResult<NodeView> paged(@PathVariable UUID centerId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        return net.daoke.openrestyplus.web.PageResult.of(list(centerId), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an Nginx node")
    public NodeView create(@PathVariable UUID centerId, @Valid @RequestBody CreateNodeRequest request) {
        requireCenter(centerId);
        var node = new NginxNode(centerId, request.name(), request.protocol(), request.host(), request.servicePort(), request.controlApiUrl());
        var saved = nodes.save(node);
        audit.success(centerId, "NGINX_NODE_CREATED", "NGINX_NODE", saved.getId());
        return NodeView.from(saved);
    }
    @PutMapping("/{nodeId}")
    public NodeView update(@PathVariable UUID centerId, @PathVariable UUID nodeId, @Valid @RequestBody UpdateNodeRequest request) {
        var node = nodes.findById(nodeId).filter(value -> value.getCenterId().equals(centerId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nginx node not found"));
        node.apply(request.name(), request.protocol(), request.host(), request.servicePort(), request.controlApiUrl(), request.enabled());
        var saved = nodes.save(node);
        audit.success(centerId, "NGINX_NODE_UPDATED", "NGINX_NODE", nodeId);
        return NodeView.from(saved);
    }
    @DeleteMapping("/{nodeId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID centerId, @PathVariable UUID nodeId) {
        var node = nodes.findById(nodeId).filter(value -> value.getCenterId().equals(centerId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nginx node not found"));
        nodes.delete(node);
        audit.success(centerId, "NGINX_NODE_DELETED", "NGINX_NODE", nodeId);
    }

    private void requireCenter(UUID centerId) {
        if (!centers.existsById(centerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found");
        }
    }

    public record CreateNodeRequest(
        @NotBlank String name,
        @NotNull NginxProtocol protocol,
        @NotBlank String host,
        @Min(1) @Max(65535) int servicePort,
        String controlApiUrl
    ) { }
    public record UpdateNodeRequest(@NotBlank String name, @NotNull NginxProtocol protocol, @NotBlank String host,
                                    @Min(1) @Max(65535) int servicePort, String controlApiUrl, boolean enabled) { }

    public record NodeView(UUID id, String name, NginxProtocol protocol, String host, int servicePort,
                           String controlApiUrl, boolean enabled) {
        static NodeView from(NginxNode node) {
            return new NodeView(node.getId(), node.getName(), node.getProtocol(), node.getHost(),
                node.getServicePort(), node.getControlApiUrl(), node.isEnabled());
        }
    }
}
