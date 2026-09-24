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
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "response_headers", nullable = false, columnDefinition = "json")
    private List<String> responseHeaders = List.of();
    protected HttpConfiguration() { }
    public HttpConfiguration(UUID centerId) { this.centerId = centerId; }
    public void apply(String rootPath, boolean hideVersion, List<String> responseHeaders) { this.rootPath=rootPath; this.hideVersion=hideVersion; this.responseHeaders=List.copyOf(responseHeaders); }
    public UUID getCenterId(){ return centerId; } public String getRootPath(){ return rootPath; } public boolean isHideVersion(){ return hideVersion; } public List<String> getResponseHeaders(){ return responseHeaders; }
}
