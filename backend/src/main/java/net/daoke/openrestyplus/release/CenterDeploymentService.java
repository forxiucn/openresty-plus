package net.daoke.openrestyplus.release;

import net.daoke.openrestyplus.nativeconfig.NativeConfigurationRenderer;
import net.daoke.openrestyplus.reload.ControlApiReloadService;
import net.daoke.openrestyplus.runtime.RuntimeConfigurationController;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Publishes the Lua snapshot, atomically writes native configuration, then reloads every enabled node. */
@Service
public class CenterDeploymentService {
    private final RuntimeConfigurationController runtimeConfigurations;
    private final NativeConfigurationRenderer nativeConfigurations;
    private final ControlApiReloadService reloads;

    public CenterDeploymentService(RuntimeConfigurationController runtimeConfigurations, NativeConfigurationRenderer nativeConfigurations, ControlApiReloadService reloads) {
        this.runtimeConfigurations = runtimeConfigurations;
        this.nativeConfigurations = nativeConfigurations;
        this.reloads = reloads;
    }

    public DeploymentView deploy(UUID centerId) {
        var published = runtimeConfigurations.publish(centerId);
        var materialized = nativeConfigurations.materialize(centerId);
        var reload = reloads.reloadCenter(centerId);
        return new DeploymentView(published.versionNo(), published.changed(), materialized.configuration().checksum(), reload);
    }

    public record DeploymentView(long runtimeVersionNo, boolean runtimeVersionChanged, String nativeConfigurationChecksum,
                                 ControlApiReloadService.ReloadTaskView reload) { }
}
