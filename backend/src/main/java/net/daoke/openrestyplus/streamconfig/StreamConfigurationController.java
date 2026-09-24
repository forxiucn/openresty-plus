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
  @PostMapping("/upstreams") @ResponseStatus(HttpStatus.CREATED) public UpstreamView createUpstream(@PathVariable UUID centerId, @Valid @RequestBody UpstreamRequest request) { center(centerId); var value=upstreams.save(new StreamUpstream(centerId,request.name(),request.targetHost(),request.targetPort(),request.resolveEnabled(),request.zoneSizeKilobytes())); audit.success(centerId,"STREAM_UPSTREAM_CREATED","STREAM_UPSTREAM",value.getId()); return UpstreamView.from(value); }
  @PutMapping("/upstreams/{upstreamId}") public UpstreamView updateUpstream(@PathVariable UUID centerId,@PathVariable UUID upstreamId,@Valid @RequestBody UpstreamRequest request) { var value=upstream(centerId,upstreamId); value.apply(request.name(),request.targetHost(),request.targetPort(),request.resolveEnabled(),request.zoneSizeKilobytes()); upstreams.save(value); audit.success(centerId,"STREAM_UPSTREAM_UPDATED","STREAM_UPSTREAM",upstreamId); return UpstreamView.from(value); }
  @DeleteMapping("/upstreams/{upstreamId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteUpstream(@PathVariable UUID centerId,@PathVariable UUID upstreamId) { upstreams.delete(upstream(centerId,upstreamId)); audit.success(centerId,"STREAM_UPSTREAM_DELETED","STREAM_UPSTREAM",upstreamId); }
  @GetMapping("/servers") public List<ServerView> servers(@PathVariable UUID centerId) { center(centerId); return servers.findByCenterIdOrderByListenPortAsc(centerId).stream().map(ServerView::from).toList(); }
  @PostMapping("/servers") @ResponseStatus(HttpStatus.CREATED) public ServerView createServer(@PathVariable UUID centerId,@Valid @RequestBody ServerRequest request) { center(centerId); upstream(centerId,request.upstreamId()); var value=new StreamServer(centerId,request.serviceName(),request.listenPort(),request.protocol(),request.upstreamId(),request.accessLog(),request.errorLog()); value.applyDynamicDns(request.dynamicDnsEnabled(),request.dynamicDnsHost(),request.dynamicDnsPort()); value=servers.save(value); audit.success(centerId,"STREAM_SERVER_CREATED","STREAM_SERVER",value.getId()); return ServerView.from(value); }
  @PutMapping("/servers/{serverId}") public ServerView updateServer(@PathVariable UUID centerId,@PathVariable UUID serverId,@Valid @RequestBody ServerRequest request) { upstream(centerId,request.upstreamId()); var value=server(centerId,serverId); value.apply(request.serviceName(),request.listenPort(),request.protocol(),request.upstreamId(),request.accessLog(),request.errorLog()); value.applyDynamicDns(request.dynamicDnsEnabled(),request.dynamicDnsHost(),request.dynamicDnsPort()); servers.save(value); audit.success(centerId,"STREAM_SERVER_UPDATED","STREAM_SERVER",serverId); return ServerView.from(value); }
  @DeleteMapping("/servers/{serverId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteServer(@PathVariable UUID centerId,@PathVariable UUID serverId) { servers.delete(server(centerId,serverId)); audit.success(centerId,"STREAM_SERVER_DELETED","STREAM_SERVER",serverId); }
  @PutMapping("/servers/{serverId}/dynamic-dns") public ServerView dynamicDns(@PathVariable UUID centerId,@PathVariable UUID serverId,@Valid @RequestBody DynamicDnsRequest request){var value=server(centerId,serverId);if(request.enabled()&&(request.host()==null||request.host().isBlank()||request.port()==null))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"启用动态 DNS 时必须提供域名和端口");value.applyDynamicDns(request.enabled(),request.host(),request.port());return ServerView.from(servers.save(value));}
  private void center(UUID id) { if(!centers.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Center not found"); }
  private StreamUpstream upstream(UUID centerId, UUID id) { center(centerId); return upstreams.findByIdAndCenterId(id,centerId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Stream upstream not found")); }
  private StreamServer server(UUID centerId, UUID id) { center(centerId); return servers.findByIdAndCenterId(id,centerId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Stream server not found")); }
  public record UpstreamRequest(@NotBlank String name,@NotBlank String targetHost,@Min(1) @Max(65535) int targetPort,boolean resolveEnabled,@Min(8) @Max(65536) int zoneSizeKilobytes) { }
  public record ServerRequest(@NotBlank String serviceName,@Min(1) @Max(65535) int listenPort,@NotNull StreamProtocol protocol,@NotNull UUID upstreamId,String accessLog,String errorLog,boolean dynamicDnsEnabled,@jakarta.validation.constraints.Pattern(regexp="[0-9A-Za-z.-]{1,253}") String dynamicDnsHost,@Min(1)@Max(65535) Integer dynamicDnsPort) { public String accessLog(){return accessLog==null||accessLog.isBlank()?"/var/log/nginx/"+serviceName+"."+listenPort+".access.log":accessLog;} public String errorLog(){return errorLog==null||errorLog.isBlank()?"/var/log/nginx/"+serviceName+"."+listenPort+".error.log":errorLog;} @jakarta.validation.constraints.AssertTrue(message="dynamic DNS requires a host and port") public boolean isDynamicDnsValid(){return !dynamicDnsEnabled||(dynamicDnsHost!=null&&!dynamicDnsHost.isBlank()&&dynamicDnsPort!=null);} }
  public record UpstreamView(UUID id,String name,String targetHost,int targetPort,boolean resolveEnabled,int zoneSizeKilobytes) { static UpstreamView from(StreamUpstream value){return new UpstreamView(value.getId(),value.getName(),value.getTargetHost(),value.getTargetPort(),value.isResolveEnabled(),value.getZoneSizeKilobytes());} }
  public record ServerView(UUID id,String serviceName,int listenPort,StreamProtocol protocol,UUID upstreamId,String accessLog,String errorLog,boolean dynamicDnsEnabled,String dynamicDnsHost,Integer dynamicDnsPort) { static ServerView from(StreamServer value){return new ServerView(value.getId(),value.getServiceName(),value.getListenPort(),value.getProtocol(),value.getUpstreamId(),value.getAccessLog(),value.getErrorLog(),value.isDynamicDnsEnabled(),value.getDynamicDnsHost(),value.getDynamicDnsPort());} }
  public record DynamicDnsRequest(boolean enabled,@jakarta.validation.constraints.Pattern(regexp="[0-9A-Za-z.-]{1,253}") String host,@Min(1)@Max(65535) Integer port){}
}
