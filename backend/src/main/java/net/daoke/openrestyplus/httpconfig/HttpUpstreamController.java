package net.daoke.openrestyplus.httpconfig;
import jakarta.validation.Valid; import jakarta.validation.constraints.Max; import jakarta.validation.constraints.Min; import jakarta.validation.constraints.NotBlank;
import net.daoke.openrestyplus.center.CenterRepository;
import net.daoke.openrestyplus.audit.AuditService;
import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException;
import java.util.List; import java.util.UUID;

@RestController @RequestMapping("/api/centers/{centerId}/http/upstreams")
public class HttpUpstreamController {
  private final CenterRepository centers; private final HttpUpstreamRepository upstreams; private final AuditService audit;
  public HttpUpstreamController(CenterRepository centers,HttpUpstreamRepository upstreams, AuditService audit){this.centers=centers;this.upstreams=upstreams;this.audit=audit;}
  @GetMapping public List<View> list(@PathVariable UUID centerId){requireCenter(centerId);return upstreams.findByCenterIdOrderByName(centerId).stream().map(View::from).toList();}
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public View create(@PathVariable UUID centerId,@Valid @RequestBody Create request){requireCenter(centerId);var saved=upstreams.save(new HttpUpstream(centerId,request.name(),request.keepaliveConnections()));audit.success(centerId,"HTTP_UPSTREAM_CREATED","HTTP_UPSTREAM",saved.getId());return View.from(saved);}
  @PutMapping("/{upstreamId}") public View update(@PathVariable UUID centerId,@PathVariable UUID upstreamId,@Valid @RequestBody Create request){var upstream=requireUpstream(centerId,upstreamId);upstream.apply(request.name(),request.keepaliveConnections());var saved=upstreams.save(upstream);audit.success(centerId,"HTTP_UPSTREAM_UPDATED","HTTP_UPSTREAM",upstreamId);return View.from(saved);}
  @DeleteMapping("/{upstreamId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID centerId,@PathVariable UUID upstreamId){upstreams.delete(requireUpstream(centerId,upstreamId));audit.success(centerId,"HTTP_UPSTREAM_DELETED","HTTP_UPSTREAM",upstreamId);}
  private void requireCenter(UUID id){if(!centers.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Center not found");}
  private HttpUpstream requireUpstream(UUID centerId, UUID upstreamId){requireCenter(centerId);return upstreams.findById(upstreamId).filter(value->upstreams.existsByIdAndCenterId(value.getId(),centerId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"HTTP upstream not found"));}
  public record Create(@NotBlank String name,@Min(1) @Max(10000) int keepaliveConnections){}
  public record View(UUID id,String name,int keepaliveConnections){static View from(HttpUpstream value){return new View(value.getId(),value.getName(),value.getKeepaliveConnections());}}
}
