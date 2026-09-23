package net.daoke.openrestyplus.center;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import net.daoke.openrestyplus.audit.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/centers")
@Tag(name = "Centers")
public class CenterController {
    private final CenterRepository repository;
    private final AuditService audit;
    public CenterController(CenterRepository repository, AuditService audit) { this.repository = repository; this.audit = audit; }
    @GetMapping
    @Operation(summary = "List centers")
    public List<CenterView> list() { return repository.findAll().stream().map(CenterView::from).toList(); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a center")
    public CenterView create(@Valid @RequestBody CreateCenterRequest request) {
        var saved = repository.save(new Center(request.code(), request.name()));
        audit.success(saved.getId(), "CENTER_CREATED", "CENTER", saved.getId());
        return CenterView.from(saved);
    }
    @PutMapping("/{centerId}")
    public CenterView update(@PathVariable UUID centerId, @Valid @RequestBody UpdateCenterRequest request) {
        var center = repository.findById(centerId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found"));
        center.apply(request.code(), request.name(), request.enabled());
        var saved = repository.save(center);
        audit.success(centerId, "CENTER_UPDATED", "CENTER", centerId);
        return CenterView.from(saved);
    }
    @DeleteMapping("/{centerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID centerId) {
        var center = repository.findById(centerId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found"));
        repository.delete(center);
        audit.success(centerId, "CENTER_DELETED", "CENTER", centerId);
    }
    public record CreateCenterRequest(@NotBlank String code, @NotBlank String name) {}
    public record UpdateCenterRequest(@NotBlank String code, @NotBlank String name, boolean enabled) {}
    public record CenterView(UUID id, String code, String name, boolean enabled) {
        static CenterView from(Center c) { return new CenterView(c.getId(), c.getCode(), c.getName(), c.isEnabled()); }
    }
}
