package net.daoke.openrestyplus.httpconfig;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import net.daoke.openrestyplus.center.CenterRepository;
import net.daoke.openrestyplus.audit.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/centers/{centerId}/http/servers")
public class HttpServerController {
    private final CenterRepository centers;
    private final HttpServerRepository servers;
    private final HttpLocationRepository locations;
    private final HttpUpstreamRepository upstreams;
    private final AuditService audit;

    public HttpServerController(CenterRepository centers, HttpServerRepository servers,
                                HttpLocationRepository locations, HttpUpstreamRepository upstreams, AuditService audit) {
        this.centers = centers;
        this.servers = servers;
        this.locations = locations;
        this.upstreams = upstreams;
        this.audit = audit;
    }

    @GetMapping
    public List<ServerView> list(@PathVariable UUID centerId) {
        requireCenter(centerId);
        return servers.findByCenterIdOrderByDomainAscListenPortAsc(centerId).stream().map(ServerView::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServerView create(@PathVariable UUID centerId, @Valid @RequestBody CreateServerRequest request) {
        requireCenter(centerId);
        requireCenterUpstream(centerId, request.upstreamId());
        var saved = servers.save(new HttpServer(centerId, request.domain(), request.listenPort(),
            request.sslEnabled(), request.upstreamId(), request.accessLog(), request.errorLog()));
        audit.success(centerId, "HTTP_SERVER_CREATED", "HTTP_SERVER", saved.getId());
        return ServerView.from(saved);
    }

    @PutMapping("/{serverId}")
    public ServerView update(@PathVariable UUID centerId, @PathVariable UUID serverId,
                             @Valid @RequestBody CreateServerRequest request) {
        requireCenterUpstream(centerId, request.upstreamId());
        var server = requireServerEntity(centerId, serverId);
        server.apply(request.domain(), request.listenPort(), request.sslEnabled(), request.upstreamId(),
            request.accessLog(), request.errorLog());
        var saved = servers.save(server);
        audit.success(centerId, "HTTP_SERVER_UPDATED", "HTTP_SERVER", serverId);
        return ServerView.from(saved);
    }

    @DeleteMapping("/{serverId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID centerId, @PathVariable UUID serverId) {
        servers.delete(requireServerEntity(centerId, serverId));
        audit.success(centerId, "HTTP_SERVER_DELETED", "HTTP_SERVER", serverId);
    }

    @GetMapping("/{serverId}/locations")
    public List<LocationView> listLocations(@PathVariable UUID centerId, @PathVariable UUID serverId) {
        requireServer(centerId, serverId);
        return locations.findByServerIdOrderByPath(serverId).stream().map(LocationView::from).toList();
    }

    @PostMapping("/{serverId}/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationView createLocation(@PathVariable UUID centerId, @PathVariable UUID serverId,
                                       @Valid @RequestBody CreateLocationRequest request) {
        requireServer(centerId, serverId);
        requireCenterUpstream(centerId, request.upstreamId());
        var saved = locations.save(new HttpLocation(serverId, request.path(), request.methods(),
            request.contentTypes(), request.headerLengthMin(), request.headerLengthMax(), request.bodyLengthMin(),
            request.bodyLengthMax(), request.upstreamId(), request.proxyConnectTimeoutMs(),
            request.proxyReadTimeoutMs(), request.proxySendTimeoutMs()));
        audit.success(centerId, "HTTP_LOCATION_CREATED", "HTTP_LOCATION", saved.getId());
        return LocationView.from(saved);
    }

    @PutMapping("/{serverId}/locations/{locationId}")
    public LocationView updateLocation(@PathVariable UUID centerId, @PathVariable UUID serverId,
                                       @PathVariable UUID locationId, @Valid @RequestBody CreateLocationRequest request) {
        requireServer(centerId, serverId);
        requireCenterUpstream(centerId, request.upstreamId());
        var location = requireLocation(serverId, locationId);
        location.apply(request.path(), request.methods(), request.contentTypes(), request.headerLengthMin(),
            request.headerLengthMax(), request.bodyLengthMin(), request.bodyLengthMax(), request.upstreamId(),
            request.proxyConnectTimeoutMs(), request.proxyReadTimeoutMs(), request.proxySendTimeoutMs());
        var saved = locations.save(location);
        audit.success(centerId, "HTTP_LOCATION_UPDATED", "HTTP_LOCATION", locationId);
        return LocationView.from(saved);
    }

    @DeleteMapping("/{serverId}/locations/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable UUID centerId, @PathVariable UUID serverId, @PathVariable UUID locationId) {
        requireServer(centerId, serverId);
        locations.delete(requireLocation(serverId, locationId));
        audit.success(centerId, "HTTP_LOCATION_DELETED", "HTTP_LOCATION", locationId);
    }

    private void requireCenter(UUID centerId) {
        if (!centers.existsById(centerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found");
        }
    }

    private void requireCenterUpstream(UUID centerId, UUID upstreamId) {
        if (upstreamId != null && !upstreams.existsByIdAndCenterId(upstreamId, centerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upstream does not belong to this center");
        }
    }

    private void requireServer(UUID centerId, UUID serverId) {
        if (servers.findByIdAndCenterId(serverId, centerId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "HTTP server not found");
        }
    }
    private HttpServer requireServerEntity(UUID centerId, UUID serverId) {
        return servers.findByIdAndCenterId(serverId, centerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "HTTP server not found"));
    }
    private HttpLocation requireLocation(UUID serverId, UUID locationId) {
        return locations.findById(locationId).filter(value -> value.getServerId().equals(serverId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "HTTP location not found"));
    }

    public record CreateServerRequest(
        @NotBlank String domain,
        @Min(1) @Max(65535) int listenPort,
        boolean sslEnabled,
        UUID upstreamId,
        String accessLog,
        String errorLog
    ) {
        public String accessLog() {
            return accessLog == null || accessLog.isBlank()
                ? "/var/log/nginx/" + domain + "." + listenPort + ".access.log" : accessLog;
        }

        public String errorLog() {
            return errorLog == null || errorLog.isBlank()
                ? "/var/log/nginx/" + domain + "." + listenPort + ".error.log" : errorLog;
        }
    }

    public record CreateLocationRequest(
        @NotBlank String path,
        @NotEmpty List<@NotBlank String> methods,
        @NotNull List<@NotBlank String> contentTypes,
        @Min(0) int headerLengthMin,
        @Min(0) int headerLengthMax,
        @Min(0) long bodyLengthMin,
        @Min(0) long bodyLengthMax,
        @NotNull UUID upstreamId,
        @Min(1) int proxyConnectTimeoutMs,
        @Min(1) int proxyReadTimeoutMs,
        @Min(1) int proxySendTimeoutMs
    ) {
        @AssertTrue(message = "headerLengthMin must not exceed headerLengthMax")
        public boolean isHeaderLengthRangeValid() { return headerLengthMin <= headerLengthMax; }

        @AssertTrue(message = "bodyLengthMin must not exceed bodyLengthMax")
        public boolean isBodyLengthRangeValid() { return bodyLengthMin <= bodyLengthMax; }
    }

    public record ServerView(UUID id, String domain, int listenPort, boolean sslEnabled, UUID upstreamId,
                             String accessLog, String errorLog) {
        static ServerView from(HttpServer server) {
            return new ServerView(server.getId(), server.getDomain(), server.getListenPort(), server.isSslEnabled(),
                server.getUpstreamId(), server.getAccessLog(), server.getErrorLog());
        }
    }

    public record LocationView(UUID id, String path, List<String> methods, List<String> contentTypes,
                               int headerLengthMin, int headerLengthMax, long bodyLengthMin, long bodyLengthMax,
                               UUID upstreamId, int proxyConnectTimeoutMs, int proxyReadTimeoutMs,
                               int proxySendTimeoutMs) {
        static LocationView from(HttpLocation location) {
            return new LocationView(location.getId(), location.getPath(), location.getMethods(),
                location.getContentTypes(), location.getHeaderLengthMin(), location.getHeaderLengthMax(),
                location.getBodyLengthMin(), location.getBodyLengthMax(), location.getUpstreamId(),
                location.getProxyConnectTimeoutMs(), location.getProxyReadTimeoutMs(),
                location.getProxySendTimeoutMs());
        }
    }
}
