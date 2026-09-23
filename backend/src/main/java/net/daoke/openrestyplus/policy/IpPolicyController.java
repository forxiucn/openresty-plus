package net.daoke.openrestyplus.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import net.daoke.openrestyplus.center.CenterRepository;
import net.daoke.openrestyplus.audit.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/centers/{centerId}/ip-policies")
public class IpPolicyController {
    private final CenterRepository centers;
    private final IpPolicyRepository policies;
    private final AuditService audit;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IpPolicyController(CenterRepository centers, IpPolicyRepository policies, AuditService audit) {
        this.centers = centers;
        this.policies = policies;
        this.audit = audit;
    }

    @GetMapping
    public List<View> list(@PathVariable UUID centerId,
                           @RequestParam(required = false) IpPolicyScope scope,
                           @RequestParam(required = false) UUID targetResourceId) {
        requireCenter(centerId);
        if ((scope == null) != (targetResourceId == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scope and targetResourceId must be supplied together");
        }
        var values = scope == null ? policies.findByCenterIdOrderByPriorityAscIdAsc(centerId)
            : policies.findByCenterIdAndScopeAndTargetResourceIdOrderByPriorityAscIdAsc(centerId, scope, targetResourceId);
        return values.stream().map(View::from).toList();
    }

    @GetMapping("/{policyId}")
    public View get(@PathVariable UUID centerId, @PathVariable UUID policyId) {
        return View.from(requirePolicy(centerId, policyId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public View create(@PathVariable UUID centerId, @Valid @RequestBody Request request) {
        requireCenter(centerId);
        validateRules(request.ipRules());
        var saved = policies.save(new IpPolicy(centerId, request.mode(), request.priority(), request.scope(),
            request.targetResourceId(), request.enabled(), rulesNode(request.ipRules())));
        audit.success(centerId, "IP_POLICY_CREATED", "IP_POLICY", saved.getId());
        return View.from(saved);
    }

    @PutMapping("/{policyId}")
    public View update(@PathVariable UUID centerId, @PathVariable UUID policyId, @Valid @RequestBody Request request) {
        validateRules(request.ipRules());
        var policy = requirePolicy(centerId, policyId);
        policy.apply(request.mode(), request.priority(), request.scope(), request.targetResourceId(), request.enabled(), rulesNode(request.ipRules()));
        var saved = policies.save(policy);
        audit.success(centerId, "IP_POLICY_UPDATED", "IP_POLICY", policyId);
        return View.from(saved);
    }

    @DeleteMapping("/{policyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID centerId, @PathVariable UUID policyId) {
        policies.delete(requirePolicy(centerId, policyId));
        audit.success(centerId, "IP_POLICY_DELETED", "IP_POLICY", policyId);
    }

    private void validateRules(List<String> rules) {
        if (rules == null || rules.isEmpty() || rules.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ipRules must be a non-empty JSON array");
        }
    }
    private JsonNode rulesNode(List<String> rules) { return objectMapper.valueToTree(rules); }
    private void requireCenter(UUID centerId) {
        if (!centers.existsById(centerId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found");
    }
    private IpPolicy requirePolicy(UUID centerId, UUID policyId) {
        requireCenter(centerId);
        return policies.findById(policyId).filter(value -> value.getCenterId().equals(centerId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IP policy not found"));
    }

    public record Request(@NotNull PolicyMode mode, @Min(0) int priority, @NotNull IpPolicyScope scope,
                          @NotNull UUID targetResourceId, boolean enabled, @NotNull List<String> ipRules) { }
    public record View(UUID id, PolicyMode mode, int priority, IpPolicyScope scope, UUID targetResourceId,
                       boolean enabled, List<String> ipRules) {
        static View from(IpPolicy value) { return new View(value.getId(), value.getMode(), value.getPriority(),
            value.getScope(), value.getTargetResourceId(), value.isEnabled(),
            new ObjectMapper().convertValue(value.getIpRules(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() { })); }
    }
}
