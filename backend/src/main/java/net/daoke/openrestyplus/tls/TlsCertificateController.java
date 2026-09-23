package net.daoke.openrestyplus.tls;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import net.daoke.openrestyplus.audit.AuditService;
import net.daoke.openrestyplus.center.CenterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/centers/{centerId}/tls-certificates")
public class TlsCertificateController {
  private final CenterRepository centers; private final TlsCertificateRepository certificates; private final AuditService audit;
  public TlsCertificateController(CenterRepository centers,TlsCertificateRepository certificates,AuditService audit){this.centers=centers;this.certificates=certificates;this.audit=audit;}
  @GetMapping public List<View> list(@PathVariable UUID centerId){requireCenter(centerId);return certificates.findByCenterIdOrderByName(centerId).stream().map(View::from).toList();}
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public View create(@PathVariable UUID centerId,@Valid @RequestBody CreateRequest r){requireCenter(centerId);validatePem(r.certificatePem(),r.privateKeyPem());var saved=certificates.save(new TlsCertificate(centerId,r.name(),r.commonName(),r.certificatePem(),r.privateKeyPem(),r.chainPem(),r.enabled()));audit.success(centerId,"TLS_CERTIFICATE_CREATED","TLS_CERTIFICATE",saved.getId());return View.from(saved);}
  @PutMapping("/{certificateId}") public View update(@PathVariable UUID centerId,@PathVariable UUID certificateId,@Valid @RequestBody UpdateRequest r){var value=requireCertificate(centerId,certificateId);if((r.certificatePem()==null)!=(r.privateKeyPem()==null))throw bad("更新证书内容时必须同时提供证书和私钥");if(r.certificatePem()!=null)validatePem(r.certificatePem(),r.privateKeyPem());value.apply(r.name(),r.commonName(),r.certificatePem(),r.privateKeyPem(),r.chainPem(),r.enabled());certificates.save(value);audit.success(centerId,"TLS_CERTIFICATE_UPDATED","TLS_CERTIFICATE",certificateId);return View.from(value);}
  @DeleteMapping("/{certificateId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID centerId,@PathVariable UUID certificateId){certificates.delete(requireCertificate(centerId,certificateId));audit.success(centerId,"TLS_CERTIFICATE_DELETED","TLS_CERTIFICATE",certificateId);}
  private void requireCenter(UUID id){if(!centers.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Center not found");}
  private TlsCertificate requireCertificate(UUID centerId,UUID id){return certificates.findByIdAndCenterId(id,centerId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"TLS certificate not found"));}
  private static void validatePem(String cert,String key){if(!cert.contains("BEGIN CERTIFICATE")||!key.contains("BEGIN")||!key.contains("PRIVATE KEY"))throw bad("证书或私钥 PEM 内容无效");}
  private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,message);}
  public record CreateRequest(@NotBlank @Pattern(regexp="[a-z0-9][a-z0-9.-]{0,127}") String name,@NotBlank String commonName,@NotBlank String certificatePem,@NotBlank String privateKeyPem,String chainPem,boolean enabled){}
  public record UpdateRequest(@NotBlank @Pattern(regexp="[a-z0-9][a-z0-9.-]{0,127}") String name,@NotBlank String commonName,String certificatePem,String privateKeyPem,String chainPem,boolean enabled){}
  public record View(UUID id,String name,String commonName,boolean enabled,Instant createdAt,Instant updatedAt){static View from(TlsCertificate v){return new View(v.getId(),v.getName(),v.getCommonName(),v.isEnabled(),v.getCreatedAt(),v.getUpdatedAt());}}
}
