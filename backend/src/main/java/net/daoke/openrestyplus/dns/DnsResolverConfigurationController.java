package net.daoke.openrestyplus.dns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import net.daoke.openrestyplus.center.CenterRepository;
import net.daoke.openrestyplus.audit.AuditService;
import net.daoke.openrestyplus.httpconfig.HttpLocationRepository;
import net.daoke.openrestyplus.httpconfig.HttpServerRepository;
import net.daoke.openrestyplus.streamconfig.StreamServerRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController @RequestMapping("/api/centers/{centerId}/dns-resolvers")
public class DnsResolverConfigurationController {
 private final CenterRepository centers; private final DnsResolverConfigurationRepository values; private final HttpServerRepository httpServers; private final HttpLocationRepository httpLocations; private final StreamServerRepository streamServers; private final AuditService audit;
 public DnsResolverConfigurationController(CenterRepository centers,DnsResolverConfigurationRepository values,HttpServerRepository httpServers,HttpLocationRepository httpLocations,StreamServerRepository streamServers,AuditService audit){this.centers=centers;this.values=values;this.httpServers=httpServers;this.httpLocations=httpLocations;this.streamServers=streamServers;this.audit=audit;}
 @GetMapping public List<View> list(@PathVariable UUID centerId){center(centerId);return values.findByCenterIdOrderByScopeAsc(centerId).stream().map(View::from).toList();}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public View create(@PathVariable UUID centerId,@Valid @RequestBody Request r){center(centerId);validate(centerId,r,null);var saved=values.save(new DnsResolverConfiguration(centerId,r.scope(),r.targetResourceId(),r.resolverAddresses(),r.validSeconds(),r.timeoutMilliseconds(),r.ipv6Enabled(),r.enabled()));audit.success(centerId,"DNS_RESOLVER_CREATED","DNS_RESOLVER",saved.getId());return View.from(saved);}
 @PutMapping("/{id}") public View update(@PathVariable UUID centerId,@PathVariable UUID id,@Valid @RequestBody Request r){var v=values.findById(id).filter(x->x.getCenterId().equals(centerId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));validate(centerId,r,id);v.apply(r.scope(),r.targetResourceId(),r.resolverAddresses(),r.validSeconds(),r.timeoutMilliseconds(),r.ipv6Enabled(),r.enabled());var saved=values.save(v);audit.success(centerId,"DNS_RESOLVER_UPDATED","DNS_RESOLVER",id);return View.from(saved);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID centerId,@PathVariable UUID id){var value=values.findById(id).filter(x->x.getCenterId().equals(centerId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));values.delete(value);audit.success(centerId,"DNS_RESOLVER_DELETED","DNS_RESOLVER",id);}
 private void center(UUID id){if(!centers.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 private void validate(UUID centerId,Request r,UUID currentId){boolean global=r.scope()==DnsResolverScope.HTTP||r.scope()==DnsResolverScope.STREAM;if(global!=(r.targetResourceId()==null))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"全局作用域不应绑定资源，Server 和 Location 作用域必须绑定资源");boolean targetValid=switch(r.scope()){case HTTP,STREAM->true;case HTTP_SERVER->httpServers.findByIdAndCenterId(r.targetResourceId(),centerId).isPresent();case HTTP_LOCATION->httpLocations.findById(r.targetResourceId()).flatMap(x->httpServers.findByIdAndCenterId(x.getServerId(),centerId)).isPresent();case STREAM_SERVER->streamServers.findByIdAndCenterId(r.targetResourceId(),centerId).isPresent();};if(!targetValid)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Resolver 目标不属于当前中心");boolean duplicate=values.findByCenterIdOrderByScopeAsc(centerId).stream().anyMatch(x->x.getScope()==r.scope()&&Objects.equals(x.getTargetResourceId(),r.targetResourceId())&&!Objects.equals(x.getId(),currentId));if(duplicate)throw new ResponseStatusException(HttpStatus.CONFLICT,"同一作用域和目标只能配置一个 Resolver");}
 public record Request(@NotNull DnsResolverScope scope,UUID targetResourceId,@NotEmpty List<@Pattern(regexp="[0-9a-fA-F:.\\[\\]]+") String> resolverAddresses,@Min(1)@Max(3600) int validSeconds,@Min(100)@Max(60000) int timeoutMilliseconds,boolean ipv6Enabled,boolean enabled){}
 public record View(UUID id,DnsResolverScope scope,UUID targetResourceId,List<String> resolverAddresses,int validSeconds,int timeoutMilliseconds,boolean ipv6Enabled,boolean enabled){static View from(DnsResolverConfiguration v){return new View(v.getId(),v.getScope(),v.getTargetResourceId(),v.getResolverAddresses(),v.getValidSeconds(),v.getTimeoutMilliseconds(),v.isIpv6Enabled(),v.isEnabled());}}
}
