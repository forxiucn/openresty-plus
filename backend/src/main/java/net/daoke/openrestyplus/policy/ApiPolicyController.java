package net.daoke.openrestyplus.policy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.daoke.openrestyplus.center.CenterRepository;
import net.daoke.openrestyplus.audit.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/centers/{centerId}/api-policies")
public class ApiPolicyController {
    private final CenterRepository centers;
    private final ApiPolicyRepository policies;
    private final AuditService audit;

    public ApiPolicyController(CenterRepository centers, ApiPolicyRepository policies, AuditService audit) {
        this.centers = centers;
        this.policies = policies;
        this.audit = audit;
    }

    @GetMapping
    public List<View> list(@PathVariable UUID centerId, @RequestParam(required = false) UUID httpLocationId) {
        requireCenter(centerId);
        var values = httpLocationId == null ? policies.findByCenterIdOrderByPriorityAscIdAsc(centerId)
            : policies.findByCenterIdAndHttpLocationIdOrderByPriorityAscIdAsc(centerId, httpLocationId);
        return values.stream().map(View::from).toList();
    }
    @GetMapping("/{policyId}")
    public View get(@PathVariable UUID centerId, @PathVariable UUID policyId) { return View.from(requirePolicy(centerId, policyId)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public View create(@PathVariable UUID centerId, @Valid @RequestBody Request request) {
        requireCenter(centerId);
        var saved = policies.save(toPolicy(centerId, request));
        audit.success(centerId, "API_POLICY_CREATED", "API_POLICY", saved.getId());
        return View.from(saved);
    }
    @PutMapping("/{policyId}")
    public View update(@PathVariable UUID centerId, @PathVariable UUID policyId, @Valid @RequestBody Request request) {
        var policy = requirePolicy(centerId, policyId);
        policy.apply(request.mode(), request.priority(), request.httpLocationId(), request.enabled(), toRules(request.rules()));
        var saved = policies.save(policy);
        audit.success(centerId, "API_POLICY_UPDATED", "API_POLICY", policyId);
        return View.from(saved);
    }
    @DeleteMapping("/{policyId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID centerId, @PathVariable UUID policyId) {
        policies.delete(requirePolicy(centerId, policyId));
        audit.success(centerId, "API_POLICY_DELETED", "API_POLICY", policyId);
    }
    @PostMapping("/{policyId}/move")
    public List<View> move(@PathVariable UUID centerId, @PathVariable UUID policyId,
                           @RequestParam String direction) {
        var policy = requirePolicy(centerId, policyId);
        int offset = "UP".equalsIgnoreCase(direction) ? -1 : "DOWN".equalsIgnoreCase(direction) ? 1 : 0;
        if (offset == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction must be UP or DOWN");
        var scoped = policies.findByCenterIdAndHttpLocationIdOrderByPriorityAscIdAsc(centerId, policy.getHttpLocationId());
        int index = scoped.stream().map(ApiPolicy::getId).toList().indexOf(policyId);
        int next = index + offset;
        if (next >= 0 && next < scoped.size()) {
            var other = scoped.get(next);
            int currentPriority = policy.getPriority();
            policy.changePriority(other.getPriority()); other.changePriority(currentPriority);
            policies.saveAll(List.of(policy, other));
            audit.success(centerId, "API_POLICY_PRIORITY_CHANGED", "API_POLICY", policyId);
        }
        return policies.findByCenterIdOrderByPriorityAscIdAsc(centerId).stream().map(View::from).toList();
    }

    private ApiPolicy toPolicy(UUID centerId, Request request) {
        return new ApiPolicy(centerId, request.mode(), request.priority(), request.httpLocationId(), request.enabled(), toRules(request.rules()));
    }
    private List<ApiPolicyRule> toRules(List<Rule> rules) {
        return rules.stream().map(rule -> new ApiPolicyRule(rule.method().toUpperCase(Locale.ROOT), rule.path())).toList();
    }
    private void requireCenter(UUID centerId) { if (!centers.existsById(centerId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found"); }
    private ApiPolicy requirePolicy(UUID centerId, UUID policyId) {
        requireCenter(centerId);
        return policies.findById(policyId).filter(value -> value.getCenterId().equals(centerId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API policy not found"));
    }
    public record Request(@NotNull PolicyMode mode, @Min(0) int priority, @NotNull UUID httpLocationId, boolean enabled,
                          @NotEmpty @Size(max = 200) List<@Valid Rule> rules) { }
    public record Rule(@NotBlank @Size(max = 16) String method, @NotBlank @Size(max = 1024) String path) { }
    public record RuleView(String method, String path) { static RuleView from(ApiPolicyRule value) { return new RuleView(value.getMethod(), value.getPathPattern()); } }
    public record View(UUID id, PolicyMode mode, int priority, UUID httpLocationId, boolean enabled, List<RuleView> rules) {
        static View from(ApiPolicy value) { return new View(value.getId(), value.getMode(), value.getPriority(), value.getHttpLocationId(), value.isEnabled(), value.getRules().stream().map(RuleView::from).toList()); }
    }
}
