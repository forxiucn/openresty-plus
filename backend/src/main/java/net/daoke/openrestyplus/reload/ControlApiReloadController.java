package net.daoke.openrestyplus.reload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/centers/{centerId}")
@Tag(name = "Control API reload")
public class ControlApiReloadController {
    private final ControlApiReloadService reloads;

    public ControlApiReloadController(ControlApiReloadService reloads) {
        this.reloads = reloads;
    }

    @PostMapping({"/reload", "/control-api-reloads"})
    @Operation(summary = "Reload all enabled Nginx nodes in a center through their Control APIs")
    public ControlApiReloadService.ReloadTaskView reload(@PathVariable UUID centerId) {
        return reloads.reloadCenter(centerId);
    }

    @GetMapping("/control-api-reloads")
    public List<ControlApiReloadService.ReloadTaskView> list(@PathVariable UUID centerId) {
        return reloads.list(centerId);
    }

    @GetMapping("/control-api-reloads/paged")
    public net.daoke.openrestyplus.web.PageResult<ControlApiReloadService.ReloadTaskView> paged(
        @PathVariable UUID centerId,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
        return net.daoke.openrestyplus.web.PageResult.of(list(centerId), page, size);
    }

    @GetMapping("/control-api-reloads/{taskId}")
    public ControlApiReloadService.ReloadTaskView get(@PathVariable UUID centerId, @PathVariable UUID taskId) {
        return reloads.get(centerId, taskId);
    }
}
