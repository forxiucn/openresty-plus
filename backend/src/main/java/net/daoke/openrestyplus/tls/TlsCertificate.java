package net.daoke.openrestyplus.tls;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tls_certificate", uniqueConstraints = @UniqueConstraint(name = "uk_tls_certificate_center_name", columnNames = {"center_id", "name"}))
public class TlsCertificate {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @JdbcTypeCode(Types.BINARY) private UUID id;
    @Column(name = "center_id", nullable = false) @JdbcTypeCode(Types.BINARY) private UUID centerId;
    @Column(nullable = false, length = 128) private String name;
    @Column(name = "common_name", nullable = false, length = 255) private String commonName;
    @Lob @Column(name = "certificate_pem", nullable = false, columnDefinition = "MEDIUMTEXT") private String certificatePem;
    @Lob @Column(name = "private_key_pem", nullable = false, columnDefinition = "MEDIUMTEXT") private String privateKeyPem;
    @Lob @Column(name = "chain_pem", columnDefinition = "MEDIUMTEXT") private String chainPem;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    protected TlsCertificate() { }
    public TlsCertificate(UUID centerId, String name, String commonName, String certificatePem, String privateKeyPem, String chainPem, boolean enabled) { this.centerId=centerId; apply(name, commonName, certificatePem, privateKeyPem, chainPem, enabled); }
    public void apply(String name, String commonName, String certificatePem, String privateKeyPem, String chainPem, boolean enabled) { this.name=name; this.commonName=commonName; if (certificatePem != null) this.certificatePem=certificatePem; if (privateKeyPem != null) this.privateKeyPem=privateKeyPem; this.chainPem=chainPem; this.enabled=enabled; this.updatedAt=Instant.now(); }
    public UUID getId(){return id;} public UUID getCenterId(){return centerId;} public String getName(){return name;} public String getCommonName(){return commonName;} public String getCertificatePem(){return certificatePem;} public String getPrivateKeyPem(){return privateKeyPem;} public String getChainPem(){return chainPem;} public boolean isEnabled(){return enabled;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
