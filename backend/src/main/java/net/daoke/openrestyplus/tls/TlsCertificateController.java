package net.daoke.openrestyplus.tls;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.daoke.openrestyplus.audit.AuditService;
import net.daoke.openrestyplus.center.CenterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/centers/{centerId}/tls-certificates")
public class TlsCertificateController {
  private final CenterRepository centers; private final TlsCertificateRepository certificates; private final AuditService audit;
  public TlsCertificateController(CenterRepository centers,TlsCertificateRepository certificates,AuditService audit){this.centers=centers;this.certificates=certificates;this.audit=audit;}
  @GetMapping public List<View> list(@PathVariable UUID centerId){requireCenter(centerId);return certificates.findByCenterIdOrderByName(centerId).stream().map(View::from).toList();}
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public View create(@PathVariable UUID centerId,@Valid @RequestBody CreateRequest r){
    requireCenter(centerId);
    String commonName=validateAndReadCommonName(r.certificatePem(),r.privateKeyPem(),r.chainPem());
    var saved=certificates.save(new TlsCertificate(centerId,r.name().trim(),commonName,r.certificatePem().trim(),r.privateKeyPem().trim(),blankToNull(r.chainPem()),r.enabled()==null||r.enabled()));
    audit.success(centerId,"TLS_CERTIFICATE_CREATED","TLS_CERTIFICATE",saved.getId());return View.from(saved);
  }
  @PutMapping("/{certificateId}") public View update(@PathVariable UUID centerId,@PathVariable UUID certificateId,@Valid @RequestBody UpdateRequest r){
    var value=requireCertificate(centerId,certificateId);
    if((r.certificatePem()==null)!=(r.privateKeyPem()==null))throw bad("更新证书内容时必须同时提供证书和私钥");
    String commonName=value.getCommonName();
    if(r.certificatePem()!=null) commonName=validateAndReadCommonName(r.certificatePem(),r.privateKeyPem(),r.chainPem());
    value.apply(r.name().trim(),commonName,r.certificatePem()==null?null:r.certificatePem().trim(),r.privateKeyPem()==null?null:r.privateKeyPem().trim(),r.chainPem()==null?value.getChainPem():blankToNull(r.chainPem()),r.enabled()==null?value.isEnabled():r.enabled());
    certificates.save(value);audit.success(centerId,"TLS_CERTIFICATE_UPDATED","TLS_CERTIFICATE",certificateId);return View.from(value);
  }
  @DeleteMapping("/{certificateId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID centerId,@PathVariable UUID certificateId){certificates.delete(requireCertificate(centerId,certificateId));audit.success(centerId,"TLS_CERTIFICATE_DELETED","TLS_CERTIFICATE",certificateId);}
  private void requireCenter(UUID id){if(!centers.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Center not found");}
  private TlsCertificate requireCertificate(UUID centerId,UUID id){return certificates.findByIdAndCenterId(id,centerId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"TLS certificate not found"));}
  private static String validateAndReadCommonName(String cert,String key,String chain){
    if(cert==null||key==null||!cert.contains("BEGIN CERTIFICATE")||!key.contains("BEGIN")||!key.contains("PRIVATE KEY"))throw bad("证书或私钥 PEM 内容无效");
    try {
      X509Certificate certificate=(X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(cert.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
      if(chain!=null&&!chain.isBlank()&&!chain.contains("BEGIN CERTIFICATE"))throw bad("证书链 PEM 内容无效");
      String commonName=new LdapName(certificate.getSubjectX500Principal().getName()).getRdns().stream().filter(rdn->"CN".equalsIgnoreCase(rdn.getType())).map(rdn->String.valueOf(rdn.getValue())).findFirst().orElse(null);
      if(commonName==null||commonName.isBlank())throw bad("证书主题中未找到通用名称（CN）");
      return commonName;
    } catch (InvalidNameException exception) { throw bad("无法读取证书主题中的通用名称（CN）");
    } catch (java.security.cert.CertificateException exception) { throw bad("证书 PEM 内容无效或无法解析"); }
  }
  private static String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
  private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,message);}
  public record CreateRequest(@NotBlank @Size(max=128) String name,@NotBlank String certificatePem,@NotBlank String privateKeyPem,String chainPem,Boolean enabled){}
  public record UpdateRequest(@NotBlank @Size(max=128) String name,String certificatePem,String privateKeyPem,String chainPem,Boolean enabled){}
  public record View(UUID id,String name,String commonName,boolean enabled,Instant createdAt,Instant updatedAt){static View from(TlsCertificate v){return new View(v.getId(),v.getName(),v.getCommonName(),v.isEnabled(),v.getCreatedAt(),v.getUpdatedAt());}}
}
