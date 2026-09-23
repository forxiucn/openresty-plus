package net.daoke.openrestyplus.release;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/centers/{centerId}/deployments")
public class CenterDeploymentController {
    private final CenterDeploymentService deployments;
    public CenterDeploymentController(CenterDeploymentService deployments) { this.deployments = deployments; }
    @PostMapping
    public CenterDeploymentService.DeploymentView deploy(@PathVariable UUID centerId) { return deployments.deploy(centerId); }
}
