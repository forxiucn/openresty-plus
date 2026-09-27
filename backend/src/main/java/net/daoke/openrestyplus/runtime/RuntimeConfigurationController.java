package net.daoke.openrestyplus.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.daoke.openrestyplus.audit.AuditEvent;
import net.daoke.openrestyplus.audit.AuditEventRepository;
import net.daoke.openrestyplus.center.CenterRepository;
import net.daoke.openrestyplus.httpconfig.HttpLocationRepository;
import net.daoke.openrestyplus.httpconfig.HttpConfigurationRepository;
import net.daoke.openrestyplus.httpconfig.HttpServerRepository;
import net.daoke.openrestyplus.httpconfig.HttpUpstreamRepository;
import net.daoke.openrestyplus.policy.ApiPolicyRepository;
import net.daoke.openrestyplus.policy.IpPolicyRepository;
import net.daoke.openrestyplus.streamconfig.StreamServerRepository;
import net.daoke.openrestyplus.streamconfig.StreamUpstreamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Publishes immutable database snapshots that Lua workers can poll by version. */
@RestController
@RequestMapping("/api/centers/{centerId}/runtime-configurations")
public class RuntimeConfigurationController {
    private final CenterRepository centers;
    private final HttpUpstreamRepository upstreams;
    private final HttpServerRepository servers;
    private final HttpLocationRepository locations;
    private final HttpConfigurationRepository httpConfigurations;
    private final IpPolicyRepository ipPolicies;
    private final ApiPolicyRepository apiPolicies;
    private final StreamUpstreamRepository streamUpstreams;
    private final StreamServerRepository streamServers;
    private final RuntimeConfigurationVersionRepository versions;
    private final AuditEventRepository auditEvents;
    private final ObjectMapper objectMapper;

    public RuntimeConfigurationController(CenterRepository centers, HttpUpstreamRepository upstreams,
                                          HttpServerRepository servers, HttpLocationRepository locations,
                                          HttpConfigurationRepository httpConfigurations,
                                          IpPolicyRepository ipPolicies, ApiPolicyRepository apiPolicies,
                                          StreamUpstreamRepository streamUpstreams, StreamServerRepository streamServers,
                                          RuntimeConfigurationVersionRepository versions, AuditEventRepository auditEvents) {
        this.centers = centers;
        this.upstreams = upstreams;
        this.servers = servers;
        this.locations = locations;
        this.httpConfigurations = httpConfigurations;
        this.ipPolicies = ipPolicies;
        this.apiPolicies = apiPolicies;
        this.streamUpstreams = streamUpstreams;
        this.streamServers = streamServers;
        this.versions = versions;
        this.auditEvents = auditEvents;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping
    public List<VersionView> list(@PathVariable UUID centerId) {
        requireCenter(centerId);
        return versions.findByCenterIdOrderByVersionNoDesc(centerId).stream().map(VersionView::from).toList();
    }

    @GetMapping("/paged")
    public net.daoke.openrestyplus.web.PageResult<VersionView> paged(@PathVariable UUID centerId,
                                                                     @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
                                                                     @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
        return net.daoke.openrestyplus.web.PageResult.of(list(centerId), page, size);
    }

    @GetMapping("/current")
    public PublishedConfiguration current(@PathVariable UUID centerId) {
        requireCenter(centerId);
        return versions.findFirstByCenterIdOrderByVersionNoDesc(centerId)
            .map(value -> published(value, true))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No runtime configuration has been published"));
    }

    /** Returns the current center draft compared with its newest published snapshot. */
    @GetMapping("/draft")
    public DraftStatus draft(@PathVariable UUID centerId) {
        requireCenter(centerId);
        JsonNode content = objectMapper.valueToTree(currentModel(centerId));
        String checksum = sha256(content.toString());
        var latest = versions.findFirstByCenterIdOrderByVersionNoDesc(centerId);
        if (latest.isPresent() && latest.get().getChecksum().equals(checksum)) {
            return new DraftStatus(latest.get().getVersionNo(), 0, List.of(), checksum);
        }
        JsonNode published = latest.map(RuntimeConfigurationVersion::getContent).orElse(null);
        List<String> changedSections = changedSections(content, published);
        return new DraftStatus(latest.map(RuntimeConfigurationVersion::getVersionNo).orElse(null),
            changedSections.size(), changedSections, checksum);
    }

    /** Returns the submitted center draft and the newest published snapshot for a release review. */
    @GetMapping("/draft/compare")
    public DraftComparison compareDraft(@PathVariable UUID centerId) {
        requireCenter(centerId);
        JsonNode draft = objectMapper.valueToTree(currentModel(centerId));
        String checksum = sha256(draft.toString());
        var latest = versions.findFirstByCenterIdOrderByVersionNoDesc(centerId);
        JsonNode published = latest.map(RuntimeConfigurationVersion::getContent).orElse(null);
        List<String> changedSections = latest.isPresent() && latest.get().getChecksum().equals(checksum)
            ? List.of()
            : changedSections(draft, published);
        return new DraftComparison(latest.map(RuntimeConfigurationVersion::getVersionNo).orElse(null),
            asConfigurationMap(published), asConfigurationMap(draft), changedSections);
    }

    @PostMapping
    @Transactional
    public PublishedConfiguration publish(@PathVariable UUID centerId) {
        requireCenter(centerId);
        JsonNode content = objectMapper.valueToTree(currentModel(centerId));
        String checksum = sha256(content.toString());
        var latest = versions.findFirstByCenterIdOrderByVersionNoDesc(centerId);
        if (latest.isPresent() && latest.get().getChecksum().equals(checksum)) {
            return published(latest.get(), false);
        }
        long number = latest.map(item -> item.getVersionNo() + 1).orElse(1L);
        var saved = versions.save(new RuntimeConfigurationVersion(centerId, number, checksum, content, "admin"));
        auditEvents.save(new AuditEvent(centerId, "admin", "RUNTIME_CONFIGURATION_PUBLISHED", "CENTER", centerId.toString(),
            "SUCCESS", objectMapper.createObjectNode().put("versionNo", number).put("checksum", checksum)));
        return published(saved, true);
    }

    /** Creates a new immutable version from a prior snapshot; workers only ever read the newest version. */
    @PostMapping("/{versionNo}/rollback")
    @Transactional
    public PublishedConfiguration rollback(@PathVariable UUID centerId, @PathVariable long versionNo) {
        requireCenter(centerId);
        var source = versions.findByCenterIdAndVersionNo(centerId, versionNo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Runtime configuration version not found"));
        long nextNumber = versions.findFirstByCenterIdOrderByVersionNoDesc(centerId)
            .map(value -> value.getVersionNo() + 1).orElse(1L);
        var saved = versions.save(new RuntimeConfigurationVersion(centerId, nextNumber, source.getChecksum(),
            source.getContent(), "admin", "ROLLED_BACK"));
        auditEvents.save(new AuditEvent(centerId, "admin", "RUNTIME_CONFIGURATION_ROLLED_BACK", "CENTER", centerId.toString(),
            "SUCCESS", objectMapper.createObjectNode().put("sourceVersionNo", versionNo).put("versionNo", nextNumber)));
        return published(saved, true);
    }

    private Map<String, Object> currentModel(UUID centerId) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("schemaVersion", 1);
        value.put("centerId", centerId);
        value.put("httpConfiguration", httpConfigurations.findById(centerId).orElse(null));
        value.put("httpUpstreams", upstreams.findByCenterIdOrderByName(centerId));
        value.put("httpServers", servers.findByCenterIdOrderByDomainAscListenPortAsc(centerId).stream().map(server -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("server", server);
            item.put("locations", locations.findByServerIdOrderByPath(server.getId()));
            return item;
        }).toList());
        value.put("ipPolicies", ipPolicies.findByCenterIdOrderByPriorityAscIdAsc(centerId));
        value.put("apiPolicies", apiPolicies.findByCenterIdOrderByPriorityAscIdAsc(centerId));
        value.put("streamUpstreams", streamUpstreams.findByCenterIdOrderByName(centerId));
        value.put("streamServers", streamServers.findByCenterIdOrderByListenPortAsc(centerId));
        return value;
    }

    private List<String> changedSections(JsonNode draft, JsonNode published) {
        return List.of("httpConfiguration", "httpUpstreams", "httpServers", "ipPolicies", "apiPolicies", "streamUpstreams", "streamServers")
            .stream()
            .filter(section -> published == null || !draft.path(section).equals(published.path(section)))
            .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asConfigurationMap(JsonNode value) {
        return value == null ? null : objectMapper.convertValue(value, Map.class);
    }

    private void requireCenter(UUID centerId) {
        if (!centers.existsById(centerId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found");
    }

    @SuppressWarnings("unchecked")
    private PublishedConfiguration published(RuntimeConfigurationVersion value, boolean changed) {
        return new PublishedConfiguration(value.getId(), value.getVersionNo(), value.getChecksum(),
            objectMapper.convertValue(value.getContent(), Map.class), changed);
    }

    private static String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record VersionView(UUID id, long versionNo, String checksum, String state, Instant createdAt) {
        static VersionView from(RuntimeConfigurationVersion value) {
            return new VersionView(value.getId(), value.getVersionNo(), value.getChecksum(), value.getState(), value.getCreatedAt());
        }
    }

    public record PublishedConfiguration(UUID id, long versionNo, String checksum, Map<String, Object> content, boolean changed) { }
    public record DraftStatus(Long publishedVersionNo, int changeCount, List<String> changedSections, String checksum) { }
    public record DraftComparison(Long publishedVersionNo, Map<String, Object> published, Map<String, Object> draft, List<String> changedSections) { }
}
