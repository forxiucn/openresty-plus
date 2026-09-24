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
import net.daoke.openrestyplus.tls.TlsCertificateRepository;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/centers/{centerId}/http/servers")
public class HttpServerController {
    private final CenterRepository centers;
    private final HttpServerRepository servers;
    private final HttpLocationRepository locations;
    private final HttpUpstreamRepository upstreams;
    private final AuditService audit;
    private final TlsCertificateRepository certificates;

    public HttpServerController(CenterRepository centers, HttpServerRepository servers,
                                HttpLocationRepository locations, HttpUpstreamRepository upstreams, AuditService audit, TlsCertificateRepository certificates) {
        this.centers = centers;
        this.servers = servers;
        this.locations = locations;
        this.upstreams = upstreams;
        this.audit = audit;
        this.certificates = certificates;
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
        requireCertificate(centerId, request.sslEnabled(), request.certificateId());
        var saved = servers.save(new HttpServer(centerId, request.domain(), request.listenPort(),
            request.sslEnabled(), request.certificateId(), request.upstreamId(), request.accessLog(), request.errorLog()));
        audit.success(centerId, "HTTP_SERVER_CREATED", "HTTP_SERVER", saved.getId());
        return ServerView.from(saved);
    }

    @PutMapping("/{serverId}")
    public ServerView update(@PathVariable UUID centerId, @PathVariable UUID serverId,
                             @Valid @RequestBody CreateServerRequest request) {
        requireCenterUpstream(centerId, request.upstreamId());
        requireCertificate(centerId, request.sslEnabled(), request.certificateId());
        var server = requireServerEntity(centerId, serverId);
        server.apply(request.domain(), request.listenPort(), request.sslEnabled(), request.certificateId(), request.upstreamId(),
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
            request.proxyReadTimeoutMs(), request.proxySendTimeoutMs(), request.rateLimitEnabled(),
            request.ratePerSecond(), request.rateLimitBurst(), request.rateLimitNodelay()));
        saved.applyDynamicDns(request.dynamicDnsEnabled(), request.dynamicDnsHost(), request.dynamicDnsPort());
        saved = locations.save(saved);
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
            request.proxyConnectTimeoutMs(), request.proxyReadTimeoutMs(), request.proxySendTimeoutMs(),
            request.rateLimitEnabled(), request.ratePerSecond(), request.rateLimitBurst(), request.rateLimitNodelay());
        location.applyDynamicDns(request.dynamicDnsEnabled(), request.dynamicDnsHost(), request.dynamicDnsPort());
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
    @PutMapping("/{serverId}/policy-settings")
    public ServerView serverPolicySettings(@PathVariable UUID centerId, @PathVariable UUID serverId,
                                           @Valid @RequestBody ServerPolicySettingsRequest request) {
        var server = requireServerEntity(centerId, serverId);
        var before = Map.of("ipPolicyEnabled", server.isIpPolicyEnabled(), "apiPolicyEnabled", server.isApiPolicyEnabled());
        server.applyPolicySettings(request.ipPolicyEnabled(), request.apiPolicyEnabled());
        var saved = servers.save(server);
        audit.success(centerId, "HTTP_SERVER_POLICY_SETTINGS_UPDATED", "HTTP_SERVER", serverId,
            Map.of("before", before, "after", Map.of("ipPolicyEnabled", request.ipPolicyEnabled(), "apiPolicyEnabled", request.apiPolicyEnabled())));
        return ServerView.from(saved);
    }
    @PutMapping("/{serverId}/locations/{locationId}/policy-settings")
    public LocationView locationPolicySettings(@PathVariable UUID centerId, @PathVariable UUID serverId,
                                               @PathVariable UUID locationId,
                                               @Valid @RequestBody LocationPolicySettingsRequest request) {
        requireServer(centerId, serverId);
        var location = requireLocation(serverId, locationId);
        var before = Map.of("ipPolicyEnabled", location.isIpPolicyEnabled(), "apiPolicyEnabled", location.isApiPolicyEnabled());
        location.applyPolicySettings(request.ipPolicyEnabled(), request.apiPolicyEnabled());
        var saved = locations.save(location);
        audit.success(centerId, "HTTP_LOCATION_POLICY_SETTINGS_UPDATED", "HTTP_LOCATION", locationId,
            Map.of("before", before, "after", Map.of("ipPolicyEnabled", request.ipPolicyEnabled(), "apiPolicyEnabled", request.apiPolicyEnabled())));
        return LocationView.from(saved);
    }
    @PutMapping("/{serverId}/locations/{locationId}/dynamic-dns")
    public LocationView dynamicDns(@PathVariable UUID centerId,@PathVariable UUID serverId,@PathVariable UUID locationId,@Valid @RequestBody DynamicDnsRequest request){
        requireServer(centerId,serverId); var location=requireLocation(serverId,locationId); if(request.enabled()&&(request.host()==null||request.host().isBlank()||request.port()==null))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"启用动态 DNS 时必须提供域名和端口"); location.applyDynamicDns(request.enabled(),request.host(),request.port()); return LocationView.from(locations.save(location));
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
    private void requireCertificate(UUID centerId, boolean sslEnabled, UUID certificateId) {
        if (sslEnabled && certificateId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TLS server must select a certificate");
        if (certificateId != null && certificates.findByIdAndCenterId(certificateId, centerId).filter(value -> value.isEnabled() || !sslEnabled).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TLS certificate does not belong to this center or is disabled");
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
        UUID certificateId,
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
        @Min(1) int proxySendTimeoutMs,
        boolean rateLimitEnabled,
        @Min(1) @Max(100000) int ratePerSecond,
        @Min(0) @Max(100000) int rateLimitBurst,
        boolean rateLimitNodelay,
        boolean dynamicDnsEnabled,
        @jakarta.validation.constraints.Pattern(regexp = "[0-9A-Za-z.-]{1,253}") String dynamicDnsHost,
        @Min(1) @Max(65535) Integer dynamicDnsPort
    ) {
        @AssertTrue(message = "headerLengthMin must not exceed headerLengthMax")
        public boolean isHeaderLengthRangeValid() { return headerLengthMin <= headerLengthMax; }

        @AssertTrue(message = "bodyLengthMin must not exceed bodyLengthMax")
        public boolean isBodyLengthRangeValid() { return bodyLengthMin <= bodyLengthMax; }

        @AssertTrue(message = "dynamic DNS requires a host and port")
        public boolean isDynamicDnsValid() { return !dynamicDnsEnabled || (dynamicDnsHost != null && !dynamicDnsHost.isBlank() && dynamicDnsPort != null); }
    }

    public record ServerView(UUID id, String domain, int listenPort, boolean sslEnabled, UUID certificateId, UUID upstreamId,
                             String accessLog, String errorLog, boolean ipPolicyEnabled, boolean apiPolicyEnabled) {
        static ServerView from(HttpServer server) {
            return new ServerView(server.getId(), server.getDomain(), server.getListenPort(), server.isSslEnabled(),
                server.getCertificateId(), server.getUpstreamId(), server.getAccessLog(), server.getErrorLog(),
                server.isIpPolicyEnabled(), server.isApiPolicyEnabled());
        }
    }

    public record LocationView(UUID id, String path, List<String> methods, List<String> contentTypes,
                               int headerLengthMin, int headerLengthMax, long bodyLengthMin, long bodyLengthMax,
                               UUID upstreamId, int proxyConnectTimeoutMs, int proxyReadTimeoutMs,
                               int proxySendTimeoutMs, boolean rateLimitEnabled, int ratePerSecond,
                               int rateLimitBurst, boolean rateLimitNodelay, boolean dynamicDnsEnabled,
                               String dynamicDnsHost, Integer dynamicDnsPort, boolean ipPolicyEnabled,
                               boolean apiPolicyEnabled) {
        static LocationView from(HttpLocation location) {
            return new LocationView(location.getId(), location.getPath(), location.getMethods(),
                location.getContentTypes(), location.getHeaderLengthMin(), location.getHeaderLengthMax(),
                location.getBodyLengthMin(), location.getBodyLengthMax(), location.getUpstreamId(),
                location.getProxyConnectTimeoutMs(), location.getProxyReadTimeoutMs(),
                location.getProxySendTimeoutMs(), location.isRateLimitEnabled(), location.getRatePerSecond(),
                location.getRateLimitBurst(), location.isRateLimitNodelay(), location.isDynamicDnsEnabled(),
                location.getDynamicDnsHost(), location.getDynamicDnsPort(), location.isIpPolicyEnabled(),
                location.isApiPolicyEnabled());
        }
    }
    public record DynamicDnsRequest(boolean enabled,@jakarta.validation.constraints.Pattern(regexp="[0-9A-Za-z.-]{1,253}") String host,@Min(1)@Max(65535) Integer port){}
    public record ServerPolicySettingsRequest(boolean ipPolicyEnabled, boolean apiPolicyEnabled) { }
    public record LocationPolicySettingsRequest(boolean ipPolicyEnabled, boolean apiPolicyEnabled) { }
}
