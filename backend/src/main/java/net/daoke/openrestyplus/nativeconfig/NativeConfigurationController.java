package net.daoke.openrestyplus.nativeconfig;

import net.daoke.openrestyplus.audit.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Read-only preview plus an explicit materialization step for node reload orchestration. */
@RestController
@RequestMapping("/api/centers/{centerId}/native-configurations")
public class NativeConfigurationController {
    private final NativeConfigurationRenderer renderer;
    private final AuditService audit;

    public NativeConfigurationController(NativeConfigurationRenderer renderer, AuditService audit) {
        this.renderer = renderer;
        this.audit = audit;
    }

    @GetMapping("/preview")
    public NativeConfigurationRenderer.RenderedConfiguration preview(@PathVariable UUID centerId) {
        return renderer.render(centerId);
    }

    @PostMapping("/materialize")
    public MaterializedView materialize(@PathVariable UUID centerId) {
        var value = renderer.materialize(centerId);
        audit.success(centerId, "NATIVE_CONFIGURATION_MATERIALIZED", "NATIVE_CONFIGURATION", centerId);
        return new MaterializedView(value.configuration(), value.directory().toString());
    }

    public record MaterializedView(NativeConfigurationRenderer.RenderedConfiguration configuration, String directory) { }
}
