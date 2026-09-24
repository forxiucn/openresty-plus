package net.daoke.openrestyplus.httpconfig;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import net.daoke.openrestyplus.audit.AuditService;
import net.daoke.openrestyplus.center.CenterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/centers/{centerId}/http/settings")
public class HttpConfigurationController {
  private final CenterRepository centers; private final HttpConfigurationRepository values; private final AuditService audit;
  public HttpConfigurationController(CenterRepository centers,HttpConfigurationRepository values,AuditService audit){this.centers=centers;this.values=values;this.audit=audit;}
  @GetMapping public View get(@PathVariable UUID centerId){requireCenter(centerId);return View.from(values.findById(centerId).orElseGet(()->new HttpConfiguration(centerId)));}
  @PutMapping public View update(@PathVariable UUID centerId,@Valid @RequestBody Request request){requireCenter(centerId);validate(request.rootPath(),request.responseHeaders());var value=values.findById(centerId).orElseGet(()->new HttpConfiguration(centerId));value.apply(request.rootPath(),request.hideVersion(),request.responseHeaders(),request.sendfileEnabled(),request.tcpNopushEnabled(),request.tcpNodelayEnabled(),request.keepaliveTimeoutSeconds(),request.clientMaxBodySize(),request.clientHeaderBufferSize(),request.largeClientHeaderBuffers(),request.serverNamesHashBucketSize(),request.gzipEnabled(),request.gzipMinLength(),request.gzipCompLevel());value=values.save(value);audit.success(centerId,"HTTP_CONFIGURATION_UPDATED","HTTP_CONFIGURATION",centerId);return View.from(value);}
  static void validate(String path,List<String> headers){if(path!=null&&!path.isBlank()&&(!path.startsWith("/")||path.contains("..")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"路径必须是受控内容挂载目录下的绝对路径，且不能包含 ..");if(headers.stream().anyMatch(v->v==null||!v.matches("[A-Za-z0-9-]{1,64}: [^\\r\\n]{0,1024}")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"响应头格式应为 Header-Name: value，且不能包含换行符");}
  private void requireCenter(UUID id){if(!centers.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Center not found");}
  public record Request(@Pattern(regexp="^$|/[^\\r\\n]*$") String rootPath,boolean hideVersion,List<String> responseHeaders,boolean sendfileEnabled,boolean tcpNopushEnabled,boolean tcpNodelayEnabled,@jakarta.validation.constraints.Min(1)@jakarta.validation.constraints.Max(3600) int keepaliveTimeoutSeconds,@Pattern(regexp="[0-9]+[kKmMgG]?") String clientMaxBodySize,@Pattern(regexp="[0-9]+[kKmMgG]?") String clientHeaderBufferSize,@Pattern(regexp="[1-9][0-9]* [0-9]+[kKmMgG]?") String largeClientHeaderBuffers,@jakarta.validation.constraints.Min(32)@jakarta.validation.constraints.Max(65536) int serverNamesHashBucketSize,boolean gzipEnabled,@Pattern(regexp="[0-9]+[kKmMgG]?") String gzipMinLength,@jakarta.validation.constraints.Min(1)@jakarta.validation.constraints.Max(9) int gzipCompLevel){public Request{responseHeaders=responseHeaders==null?List.of():List.copyOf(responseHeaders);}}
  public record View(String rootPath,boolean hideVersion,List<String> responseHeaders,boolean sendfileEnabled,boolean tcpNopushEnabled,boolean tcpNodelayEnabled,int keepaliveTimeoutSeconds,String clientMaxBodySize,String clientHeaderBufferSize,String largeClientHeaderBuffers,int serverNamesHashBucketSize,boolean gzipEnabled,String gzipMinLength,int gzipCompLevel){static View from(HttpConfiguration v){return new View(v.getRootPath(),v.isHideVersion(),v.getResponseHeaders(),v.isSendfileEnabled(),v.isTcpNopushEnabled(),v.isTcpNodelayEnabled(),v.getKeepaliveTimeoutSeconds(),v.getClientMaxBodySize(),v.getClientHeaderBufferSize(),v.getLargeClientHeaderBuffers(),v.getServerNamesHashBucketSize(),v.isGzipEnabled(),v.getGzipMinLength(),v.getGzipCompLevel());}}
}
