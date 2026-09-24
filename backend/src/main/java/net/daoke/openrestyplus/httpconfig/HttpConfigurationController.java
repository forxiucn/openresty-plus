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
  @PutMapping public View update(@PathVariable UUID centerId,@Valid @RequestBody Request request){requireCenter(centerId);validate(request.rootPath(),request.responseHeaders());var value=values.findById(centerId).orElseGet(()->new HttpConfiguration(centerId));value.apply(request.rootPath(),request.hideVersion(),request.responseHeaders());value=values.save(value);audit.success(centerId,"HTTP_CONFIGURATION_UPDATED","HTTP_CONFIGURATION",centerId);return View.from(value);}
  static void validate(String path,List<String> headers){if(path!=null&&!path.isBlank()&&(!path.startsWith("/")||path.contains("..")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"路径必须是受控内容挂载目录下的绝对路径，且不能包含 ..");if(headers.stream().anyMatch(v->v==null||!v.matches("[A-Za-z0-9-]{1,64}: [^\\r\\n]{0,1024}")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"响应头格式应为 Header-Name: value，且不能包含换行符");}
  private void requireCenter(UUID id){if(!centers.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Center not found");}
  public record Request(@Pattern(regexp="^$|/[^\\r\\n]*$") String rootPath,boolean hideVersion,List<String> responseHeaders){public Request{responseHeaders=responseHeaders==null?List.of():List.copyOf(responseHeaders);}}
  public record View(String rootPath,boolean hideVersion,List<String> responseHeaders){static View from(HttpConfiguration v){return new View(v.getRootPath(),v.isHideVersion(),v.getResponseHeaders());}}
}
