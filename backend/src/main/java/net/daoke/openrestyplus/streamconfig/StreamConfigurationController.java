package net.daoke.openrestyplus.streamconfig;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.daoke.openrestyplus.audit.AuditService;
import net.daoke.openrestyplus.center.CenterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/centers/{centerId}/stream")
public class StreamConfigurationController {
  private final CenterRepository centers; private final StreamUpstreamRepository upstreams; private final StreamServerRepository servers; private final AuditService audit;
  public StreamConfigurationController(CenterRepository centers, StreamUpstreamRepository upstreams, StreamServerRepository servers, AuditService audit) { this.centers=centers; this.upstreams=upstreams; this.servers=servers; this.audit=audit; }
  @GetMapping("/upstreams") public List<UpstreamView> upstreams(@PathVariable UUID centerId) { center(centerId); return upstreams.findByCenterIdOrderByName(centerId).stream().map(UpstreamView::from).toList(); }
  @PostMapping("/upstreams") @ResponseStatus(HttpStatus.CREATED) public UpstreamView createUpstream(@PathVariable UUID centerId, @Valid @RequestBody UpstreamRequest request) { center(centerId); var value=upstreams.save(new StreamUpstream(centerId,request.name(),request.targetHost(),request.targetPort())); audit.success(centerId,"STREAM_UPSTREAM_CREATED","STREAM_UPSTREAM",value.getId()); return UpstreamView.from(value); }
  @PutMapping("/upstreams/{upstreamId}") public UpstreamView updateUpstream(@PathVariable UUID centerId,@PathVariable UUID upstreamId,@Valid @RequestBody UpstreamRequest request) { var value=upstream(centerId,upstreamId); value.apply(request.name(),request.targetHost(),request.targetPort()); upstreams.save(value); audit.success(centerId,"STREAM_UPSTREAM_UPDATED","STREAM_UPSTREAM",upstreamId); return UpstreamView.from(value); }
  @DeleteMapping("/upstreams/{upstreamId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteUpstream(@PathVariable UUID centerId,@PathVariable UUID upstreamId) { upstreams.delete(upstream(centerId,upstreamId)); audit.success(centerId,"STREAM_UPSTREAM_DELETED","STREAM_UPSTREAM",upstreamId); }
  @GetMapping("/servers") public List<ServerView> servers(@PathVariable UUID centerId) { center(centerId); return servers.findByCenterIdOrderByListenPortAsc(centerId).stream().map(ServerView::from).toList(); }
  @PostMapping("/servers") @ResponseStatus(HttpStatus.CREATED) public ServerView createServer(@PathVariable UUID centerId,@Valid @RequestBody ServerRequest request) { center(centerId); upstream(centerId,request.upstreamId()); var value=servers.save(new StreamServer(centerId,request.serviceName(),request.listenPort(),request.protocol(),request.upstreamId(),request.accessLog(),request.errorLog())); audit.success(centerId,"STREAM_SERVER_CREATED","STREAM_SERVER",value.getId()); return ServerView.from(value); }
  @PutMapping("/servers/{serverId}") public ServerView updateServer(@PathVariable UUID centerId,@PathVariable UUID serverId,@Valid @RequestBody ServerRequest request) { upstream(centerId,request.upstreamId()); var value=server(centerId,serverId); value.apply(request.serviceName(),request.listenPort(),request.protocol(),request.upstreamId(),request.accessLog(),request.errorLog()); servers.save(value); audit.success(centerId,"STREAM_SERVER_UPDATED","STREAM_SERVER",serverId); return ServerView.from(value); }
  @DeleteMapping("/servers/{serverId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteServer(@PathVariable UUID centerId,@PathVariable UUID serverId) { servers.delete(server(centerId,serverId)); audit.success(centerId,"STREAM_SERVER_DELETED","STREAM_SERVER",serverId); }
  private void center(UUID id) { if(!centers.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Center not found"); }
  private StreamUpstream upstream(UUID centerId, UUID id) { center(centerId); return upstreams.findByIdAndCenterId(id,centerId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Stream upstream not found")); }
  private StreamServer server(UUID centerId, UUID id) { center(centerId); return servers.findByIdAndCenterId(id,centerId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Stream server not found")); }
  public record UpstreamRequest(@NotBlank String name,@NotBlank String targetHost,@Min(1) @Max(65535) int targetPort) { }
  public record ServerRequest(@NotBlank String serviceName,@Min(1) @Max(65535) int listenPort,@NotNull StreamProtocol protocol,@NotNull UUID upstreamId,String accessLog,String errorLog) { public String accessLog(){return accessLog==null||accessLog.isBlank()?"/var/log/nginx/"+serviceName+"."+listenPort+".access.log":accessLog;} public String errorLog(){return errorLog==null||errorLog.isBlank()?"/var/log/nginx/"+serviceName+"."+listenPort+".error.log":errorLog;} }
  public record UpstreamView(UUID id,String name,String targetHost,int targetPort) { static UpstreamView from(StreamUpstream value){return new UpstreamView(value.getId(),value.getName(),value.getTargetHost(),value.getTargetPort());} }
  public record ServerView(UUID id,String serviceName,int listenPort,StreamProtocol protocol,UUID upstreamId,String accessLog,String errorLog) { static ServerView from(StreamServer value){return new ServerView(value.getId(),value.getServiceName(),value.getListenPort(),value.getProtocol(),value.getUpstreamId(),value.getAccessLog(),value.getErrorLog());} }
}
