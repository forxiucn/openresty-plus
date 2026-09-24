package net.daoke.openrestyplus.httpconfig;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/** Center-wide directives rendered inside the HTTP block. Paths are relative to the node content mount. */
@Entity
@Table(name = "http_configuration")
public class HttpConfiguration {
    @Id @Column(name = "center_id") private UUID centerId;
    @Column(name = "root_path", length = 512) private String rootPath;
    @Column(name = "hide_version", nullable = false) private boolean hideVersion = true;
    @Column(name="sendfile_enabled", nullable=false) private boolean sendfileEnabled=true;
    @Column(name="tcp_nopush_enabled", nullable=false) private boolean tcpNopushEnabled=true;
    @Column(name="tcp_nodelay_enabled", nullable=false) private boolean tcpNodelayEnabled=true;
    @Column(name="keepalive_timeout_seconds", nullable=false) private int keepaliveTimeoutSeconds=65;
    @Column(name="client_max_body_size", nullable=false) private String clientMaxBodySize="10m";
    @Column(name="client_header_buffer_size", nullable=false) private String clientHeaderBufferSize="1k";
    @Column(name="large_client_header_buffers", nullable=false) private String largeClientHeaderBuffers="4 8k";
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "response_headers", nullable = false, columnDefinition = "json")
    private List<String> responseHeaders = List.of();
    protected HttpConfiguration() { }
    public HttpConfiguration(UUID centerId) { this.centerId = centerId; }
    public void apply(String rootPath, boolean hideVersion, List<String> responseHeaders, boolean sendfileEnabled, boolean tcpNopushEnabled, boolean tcpNodelayEnabled, int keepaliveTimeoutSeconds, String clientMaxBodySize, String clientHeaderBufferSize, String largeClientHeaderBuffers) { this.rootPath=rootPath; this.hideVersion=hideVersion; this.responseHeaders=List.copyOf(responseHeaders);this.sendfileEnabled=sendfileEnabled;this.tcpNopushEnabled=tcpNopushEnabled;this.tcpNodelayEnabled=tcpNodelayEnabled;this.keepaliveTimeoutSeconds=keepaliveTimeoutSeconds;this.clientMaxBodySize=clientMaxBodySize;this.clientHeaderBufferSize=clientHeaderBufferSize;this.largeClientHeaderBuffers=largeClientHeaderBuffers; }
    public UUID getCenterId(){ return centerId; } public String getRootPath(){ return rootPath; } public boolean isHideVersion(){ return hideVersion; } public List<String> getResponseHeaders(){ return responseHeaders; } public boolean isSendfileEnabled(){return sendfileEnabled;} public boolean isTcpNopushEnabled(){return tcpNopushEnabled;} public boolean isTcpNodelayEnabled(){return tcpNodelayEnabled;} public int getKeepaliveTimeoutSeconds(){return keepaliveTimeoutSeconds;} public String getClientMaxBodySize(){return clientMaxBodySize;} public String getClientHeaderBufferSize(){return clientHeaderBufferSize;} public String getLargeClientHeaderBuffers(){return largeClientHeaderBuffers;}
}
