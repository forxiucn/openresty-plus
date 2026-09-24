package net.daoke.openrestyplus.nativeconfig;

import net.daoke.openrestyplus.center.CenterRepository;
import net.daoke.openrestyplus.dns.DnsResolverConfiguration;
import net.daoke.openrestyplus.dns.DnsResolverConfigurationRepository;
import net.daoke.openrestyplus.dns.DnsResolverScope;
import net.daoke.openrestyplus.httpconfig.HttpLocation;
import net.daoke.openrestyplus.httpconfig.HttpLocationRepository;
import net.daoke.openrestyplus.httpconfig.HttpConfigurationRepository;
import net.daoke.openrestyplus.httpconfig.HttpServer;
import net.daoke.openrestyplus.httpconfig.HttpServerRepository;
import net.daoke.openrestyplus.httpconfig.HttpUpstream;
import net.daoke.openrestyplus.httpconfig.HttpUpstreamRepository;
import net.daoke.openrestyplus.httpconfig.HttpUpstreamTarget;
import net.daoke.openrestyplus.httpconfig.HttpUpstreamTargetRepository;
import net.daoke.openrestyplus.tls.TlsCertificate;
import net.daoke.openrestyplus.tls.TlsCertificateRepository;
import net.daoke.openrestyplus.streamconfig.StreamProtocol;
import net.daoke.openrestyplus.streamconfig.StreamServer;
import net.daoke.openrestyplus.streamconfig.StreamServerRepository;
import net.daoke.openrestyplus.streamconfig.StreamUpstream;
import net.daoke.openrestyplus.streamconfig.StreamUpstreamRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns the database model into a complete include tree.  The resulting tree is
 * deliberately independent from the control plane process: a node orchestrator
 * can copy {@link RenderedConfiguration#files()} to its generated-config mount,
 * test it, and only then ask Nginx to reload it.
 */
@Service
public class NativeConfigurationRenderer {
    public static final String NODE_GENERATED_ROOT = "/etc/openresty/generated";
    private static final DateTimeFormatter METADATA_TIME = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private final CenterRepository centers;
    private final HttpUpstreamRepository httpUpstreams;
    private final HttpUpstreamTargetRepository httpUpstreamTargets;
    private final HttpServerRepository httpServers;
    private final HttpLocationRepository httpLocations;
    private final TlsCertificateRepository tlsCertificates;
    private final StreamUpstreamRepository streamUpstreams;
    private final StreamServerRepository streamServers;
    private final DnsResolverConfigurationRepository dnsResolvers;
    private final HttpConfigurationRepository httpConfigurations;
    private final Path renderRoot;

    public NativeConfigurationRenderer(CenterRepository centers,
                                       HttpUpstreamRepository httpUpstreams,
                                       HttpUpstreamTargetRepository httpUpstreamTargets,
                                       HttpServerRepository httpServers,
                                       HttpLocationRepository httpLocations,
                                       TlsCertificateRepository tlsCertificates,
                                       StreamUpstreamRepository streamUpstreams,
                                       StreamServerRepository streamServers,
                                       DnsResolverConfigurationRepository dnsResolvers,
                                       HttpConfigurationRepository httpConfigurations,
                                       @Value("${OPENRESTY_RENDER_ROOT:/tmp/openresty-plus/rendered}") String renderRoot) {
        this.centers = centers;
        this.httpUpstreams = httpUpstreams;
        this.httpUpstreamTargets = httpUpstreamTargets;
        this.httpServers = httpServers;
        this.httpLocations = httpLocations;
        this.tlsCertificates = tlsCertificates;
        this.streamUpstreams = streamUpstreams;
        this.streamServers = streamServers;
        this.dnsResolvers = dnsResolvers;
        this.httpConfigurations = httpConfigurations;
        this.renderRoot = Path.of(renderRoot).toAbsolutePath().normalize();
    }

    public RenderedConfiguration render(UUID centerId) {
        var center = centers.findById(centerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Center not found"));
        if (!center.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Center is disabled and cannot be rendered");
        }

        Instant now = Instant.now();
        String nodeRoot = NODE_GENERATED_ROOT + "/" + centerId;
        Map<String, String> files = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        Map<UUID, HttpUpstream> httpUpstreamById = index(httpUpstreams.findByCenterIdOrderByName(centerId));
        Map<UUID, StreamUpstream> streamUpstreamById = index(streamUpstreams.findByCenterIdOrderByName(centerId));
        List<HttpServer> httpServerValues = httpServers.findByCenterIdOrderByDomainAscListenPortAsc(centerId);
        List<HttpLocation> rateLimitedLocations = new ArrayList<>();
        Map<UUID, TlsCertificate> certificateById = new LinkedHashMap<>();
        for (TlsCertificate certificate : tlsCertificates.findByCenterIdOrderByName(centerId)) certificateById.put(certificate.getId(), certificate);
        List<StreamServer> streamServerValues = streamServers.findByCenterIdOrderByListenPortAsc(centerId);
        List<DnsResolverConfiguration> resolverValues = dnsResolvers.findByCenterIdOrderByScopeAsc(centerId).stream().filter(DnsResolverConfiguration::isEnabled).toList();
        var httpConfiguration = httpConfigurations.findById(centerId).orElse(null);

        for (HttpUpstream upstream : httpUpstreamById.values()) {
            requireName(upstream.getName(), "HTTP upstream name");
            files.put("http/upstream/" + upstream.getName() + ".conf", httpUpstream(upstream, httpUpstreamTargets.findByUpstreamIdOrderByTargetHostAscTargetPortAsc(upstream.getId()), now));
        }
        for (HttpServer server : httpServerValues) {
            requireHost(server.getDomain(), "HTTP server domain");
            if (server.getUpstreamId() != null && !httpUpstreamById.containsKey(server.getUpstreamId())) {
                throw invalid("HTTP server " + server.getDomain() + ":" + server.getListenPort() + " references an upstream outside this center");
            }
            String serverStem = server.getDomain() + "." + server.getListenPort() + (server.isSslEnabled() ? ".ssl" : "");
            List<HttpLocation> locations = httpLocations.findByServerIdOrderByPath(server.getId());
            for (HttpLocation location : locations) {
                requireLocationPath(location.getPath());
                if (location.getAction() == net.daoke.openrestyplus.httpconfig.LocationAction.PROXY && !httpUpstreamById.containsKey(location.getUpstreamId())) {
                    throw invalid("HTTP location " + location.getPath() + " references an upstream outside this center");
                }
                if (location.isRateLimitEnabled()) rateLimitedLocations.add(location);
            files.put("http/location/" + serverStem + "/" + locationFileName(location.getPath()) + ".conf",
                    httpLocation(server, location, httpUpstreamById.get(location.getUpstreamId()), resolver(resolverValues, DnsResolverScope.HTTP_LOCATION, location.getId()), nodeRoot, now));
            }
            TlsCertificate certificate = null;
            if (server.isSslEnabled()) {
                certificate = certificateById.get(server.getCertificateId());
                if (certificate == null || !certificate.isEnabled()) throw invalid("TLS server " + server.getDomain() + " must reference an enabled center certificate");
                String certificateStem = "certificates/" + certificate.getId();
                files.put(certificateStem + ".crt", certificate.getCertificatePem() + (certificate.getChainPem() == null || certificate.getChainPem().isBlank() ? "" : "\n" + certificate.getChainPem()));
                files.put(certificateStem + ".key", certificate.getPrivateKeyPem());
            }
            files.put("http/server/" + serverStem + ".conf", httpServer(server, locations, nodeRoot, certificate, resolver(resolverValues, DnsResolverScope.HTTP_SERVER, server.getId()), now));
        }
        for (StreamUpstream upstream : streamUpstreamById.values()) {
            requireName(upstream.getName(), "Stream upstream name");
            requireHostOrAddress(upstream.getTargetHost(), "Stream upstream target host");
            files.put("stream/upstream/" + upstream.getName() + ".conf", streamUpstream(upstream, now));
        }
        for (StreamServer server : streamServerValues) {
            requireName(server.getServiceName(), "Stream service name");
            if (!streamUpstreamById.containsKey(server.getUpstreamId())) {
                throw invalid("Stream server " + server.getServiceName() + ":" + server.getListenPort() + " references an upstream outside this center");
            }
            files.put("stream/server/" + server.getServiceName() + "." + server.getListenPort()
                    + (server.getProtocol() == StreamProtocol.UDP ? ".udp" : "") + ".conf", streamServer(server, streamUpstreamById.get(server.getUpstreamId()), resolver(resolverValues, DnsResolverScope.STREAM_SERVER, server.getId()), now));
        }
        files.put("nginx.conf", rootConfiguration(nodeRoot, now, rateLimitedLocations,
                streamServerValues.stream().filter(StreamServer::isDynamicDnsEnabled).toList(),
                resolver(resolverValues, DnsResolverScope.HTTP, null), httpConfiguration,
                resolver(resolverValues, DnsResolverScope.STREAM, null)));
        return new RenderedConfiguration(centerId, center.getCode(), now, nodeRoot + "/nginx.conf", files, warnings, checksum(files));
    }

    /** Atomically replaces the center's local render tree for a deploy worker to consume. */
    public MaterializedConfiguration materialize(UUID centerId) {
        RenderedConfiguration configuration = render(centerId);
        Path centerDirectory = renderRoot.resolve(centerId.toString());
        Path staging = renderRoot.resolve("." + centerId + "." + UUID.randomUUID());
        Path old = renderRoot.resolve("." + centerId + ".previous");
        try {
            for (var entry : configuration.files().entrySet()) {
                Path target = staging.resolve(entry.getKey()).normalize();
                if (!target.startsWith(staging)) {
                    throw invalid("Unsafe generated file path");
                }
                Files.createDirectories(target.getParent());
                Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
            }
            Files.createDirectories(renderRoot);
            deleteTree(old);
            if (Files.exists(centerDirectory)) {
                Files.move(centerDirectory, old, StandardCopyOption.REPLACE_EXISTING);
            }
            move(staging, centerDirectory);
            deleteTree(old);
            return new MaterializedConfiguration(configuration, centerDirectory);
        } catch (IOException exception) {
            deleteTree(staging);
            if (!Files.exists(centerDirectory) && Files.exists(old)) {
                try { move(old, centerDirectory); } catch (IOException ignored) { }
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot materialize native Nginx configuration", exception);
        }
    }

    private String rootConfiguration(String nodeRoot, Instant now, List<HttpLocation> rateLimitedLocations,
                                     List<StreamServer> dynamicStreamServers,
                                     DnsResolverConfiguration httpResolver, net.daoke.openrestyplus.httpconfig.HttpConfiguration httpConfiguration,
                                     DnsResolverConfiguration streamResolver) {
        StringBuilder rateZones = new StringBuilder();
        for (HttpLocation location : rateLimitedLocations) {
            rateZones.append("    # 接口限速：按客户端 IP 统计，规则由 Location 配置维护。\n")
                .append("    limit_req_zone $binary_remote_addr zone=").append(rateZone(location)).append(":10m rate=")
                .append(location.getRatePerSecond()).append("r/s;\n");
        }
        StringBuilder streamTargetMaps = new StringBuilder();
        if (!dynamicStreamServers.isEmpty()) {
            streamTargetMaps.append("    # 四层动态域名目标：按监听端口选择域名，Resolver 在缓存到期后重新解析。\n")
                .append("    map $server_port $openresty_plus_stream_target {\n")
                .append("        default \"\";\n");
        }
        for (StreamServer server : dynamicStreamServers) {
            if (server.getDynamicDnsHost() == null || server.getDynamicDnsPort() == null) {
                throw invalid("Dynamic DNS stream server requires host and port");
            }
            requireHostOrAddress(server.getDynamicDnsHost(), "Stream dynamic DNS host");
            streamTargetMaps.append("        ").append(server.getListenPort()).append(' ')
                .append(server.getDynamicDnsHost()).append(";\n");
        }
        if (!dynamicStreamServers.isEmpty()) streamTargetMaps.append("    }\n");
        return header("nginx.conf", now)
            + "worker_processes auto;\n"
            + "# 工作进程与事件模型由平台统一维护，业务配置通过下方 include 加载。\n"
            + "error_log /dev/stderr notice;\n"
            + "pid /var/run/nginx.pid;\n\n"
            + "env RUNTIME_CONTROL_PLANE_URL;\n"
            + "env RUNTIME_CENTER_ID;\n"
            + "env RUNTIME_POLL_INTERVAL_SECONDS;\n\n"
            + "events { worker_connections 1024; }\n\n"
            + "http {\n"
            + "    # 七层 HTTP 配置：上游、虚拟主机和接口配置分别独立存放。\n"
            + httpBlockDirectives(httpConfiguration)
            + rootDirective(httpConfiguration == null ? null : httpConfiguration.getRootPath(), "    ")
            + versionDirective(httpConfiguration == null || httpConfiguration.isHideVersion(), "    ")
            + responseHeaders(httpConfiguration == null ? List.of() : httpConfiguration.getResponseHeaders(), "    ")
            + resolverDirective(httpResolver, "    ", true)
            + "    lua_package_path \"/usr/local/openresty/lualib/?.lua;/etc/openresty/lua/?.lua;;\";\n"
            + "    lua_package_cpath \"/usr/local/openresty/lualib/?.so;;\";\n"
            + "    lua_shared_dict runtime_configuration 10m;\n"
            + "    init_worker_by_lua_block { require(\"runtime\").start() }\n"
            + "    include /usr/local/openresty/nginx/conf/mime.types;\n"
            + "    default_type application/octet-stream;\n"
            + "    log_format openresty_plus '$remote_addr - $remote_user [$time_local] \\\"$request\\\" $status $body_bytes_sent';\n"
            + rateZones
            + "    include " + nodeRoot + "/http/upstream/*.conf;\n"
            + "    include " + nodeRoot + "/http/server/*.conf;\n"
            + "}\n\n"
            + "stream {\n"
            + "    # 四层 TCP/UDP 配置：上游和监听服务分别独立存放。\n"
            + resolverDirective(streamResolver, "    ", false)
            + streamTargetMaps
            + "    lua_package_path \"/usr/local/openresty/lualib/?.lua;/etc/openresty/lua/?.lua;;\";\n"
            + "    lua_package_cpath \"/usr/local/openresty/lualib/?.so;;\";\n"
            + "    log_format openresty_plus_stream '$remote_addr [$time_local] $protocol $status $bytes_sent $bytes_received $session_time';\n"
            + "    include " + nodeRoot + "/stream/upstream/*.conf;\n"
            + "    include " + nodeRoot + "/stream/server/*.conf;\n"
            + "}\n";
    }

    private static String httpBlockDirectives(net.daoke.openrestyplus.httpconfig.HttpConfiguration value) {
        if (value == null) return "    sendfile on;\n    tcp_nopush on;\n    tcp_nodelay on;\n    keepalive_timeout 65s;\n    client_max_body_size 10m;\n    client_header_buffer_size 1k;\n    large_client_header_buffers 4 8k;\n    server_names_hash_bucket_size 512;\n    gzip on;\n    gzip_min_length 1k;\n    gzip_comp_level 2;\n";
        return "    # 基础与性能、请求处理与客户端限制：由 HTTP 配置页面维护。\n"
            + "    sendfile " + (value.isSendfileEnabled() ? "on" : "off") + ";\n"
            + "    tcp_nopush " + (value.isTcpNopushEnabled() ? "on" : "off") + ";\n"
            + "    tcp_nodelay " + (value.isTcpNodelayEnabled() ? "on" : "off") + ";\n"
            + "    keepalive_timeout " + value.getKeepaliveTimeoutSeconds() + "s;\n"
            + "    client_max_body_size " + value.getClientMaxBodySize() + ";\n"
            + "    client_header_buffer_size " + value.getClientHeaderBufferSize() + ";\n"
            + "    large_client_header_buffers " + value.getLargeClientHeaderBuffers() + ";\n"
            + "    server_names_hash_bucket_size " + value.getServerNamesHashBucketSize() + ";\n"
            + "    gzip " + (value.isGzipEnabled() ? "on" : "off") + ";\n"
            + "    gzip_min_length " + value.getGzipMinLength() + ";\n"
            + "    gzip_comp_level " + value.getGzipCompLevel() + ";\n";
    }

    private String httpUpstream(HttpUpstream upstream, List<HttpUpstreamTarget> targets, Instant now) {
        boolean dynamicResolution = targets.stream().anyMatch(target -> target.isEnabled() && target.isResolveEnabled());
        return header("http.upstream." + upstream.getName() + ".conf", now)
            + "# HTTP 上游服务：" + upstream.getName() + "。\n"
            + "upstream " + upstream.getName() + " {\n"
            + (dynamicResolution
                ? "    # 启用动态域名解析时，Upstream 必须使用共享内存保存运行时后端地址。\n"
                    + "    zone " + upstream.getName() + " " + upstream.getZoneSizeKilobytes() + "k;\n"
                : "")
            + upstreamTargets(targets)
            + "    keepalive " + upstream.getKeepaliveConnections() + ";\n"
            + "}\n";
    }

    private String upstreamTargets(List<HttpUpstreamTarget> targets) {
        var active = targets.stream().filter(HttpUpstreamTarget::isEnabled).toList();
        if (active.isEmpty()) return "    # 尚未配置启用的后端实例，使用 down 占位防止误转发。\n    server 127.0.0.1:1 down max_fails=3 fail_timeout=10s;\n";
        StringBuilder value = new StringBuilder();
        for (HttpUpstreamTarget target : active) {
            value.append("    # 后端实例：").append(target.getTargetHost()).append(':').append(target.getTargetPort()).append("。\n")
                .append("    server ").append(hostPort(target.getTargetHost(), target.getTargetPort()))
                .append(" weight=").append(target.getWeight()).append(" max_fails=").append(target.getMaxFails())
                .append(" fail_timeout=").append(target.getFailTimeoutSeconds()).append('s');
            if (target.isResolveEnabled()) value.append(" resolve");
            if (target.isBackup()) value.append(" backup");
            value.append(";\n");
        }
        return value.toString();
    }

    private String httpServer(HttpServer server, List<HttpLocation> locations, String nodeRoot, TlsCertificate certificate, DnsResolverConfiguration resolver, Instant now) {
        String stem = server.getDomain() + "." + server.getListenPort() + (server.isSslEnabled() ? ".ssl" : "");
        StringBuilder result = new StringBuilder(header("http.server." + stem + ".conf", now));
        result.append("# HTTP 虚拟主机：").append(server.getDomain()).append(':').append(server.getListenPort()).append("。\n")
            .append("# 每个监听端口独立配置，接口规则通过 include 载入。\n");
        result.append("server {\n")
            .append("    listen ").append(server.getListenPort()).append(server.isSslEnabled() ? " ssl" : "").append(";\n")
            .append("    server_name ").append(server.getDomain()).append(";\n")
            .append("    access_log ").append(directivePath(server.getAccessLog())).append(" openresty_plus;\n")
            .append("    error_log ").append(directivePath(server.getErrorLog())).append(" warn;\n");
        result.append(rootDirective(server.getRootPath(), "    "));
        result.append(versionDirective(server.isHideVersion(), "    "));
        result.append(responseHeaders(server.getResponseHeaders(), "    "));
        result.append(resolverDirective(resolver, "    ", false));
        if (server.isSslEnabled()) {
            String certificateStem = nodeRoot + "/certificates/" + certificate.getId();
            result.append("    # TLS 证书：").append(certificate.getName()).append("（").append(certificate.getCommonName()).append("）。\n")
                .append("    ssl_certificate ").append(certificateStem).append(".crt;\n")
                .append("    ssl_certificate_key ").append(certificateStem).append(".key;\n")
                .append("    # TLS 加固：仅允许 TLS 1.2/1.3，关闭会话票据并限制会话有效期。\n")
                .append("    ssl_protocols TLSv1.2 TLSv1.3;\n")
                .append("    ssl_session_tickets off;\n")
                .append("    ssl_session_timeout 1h;\n")
                .append("    add_header Strict-Transport-Security \"max-age=31536000; includeSubDomains\" always;\n");
        }
        for (HttpLocation location : locations) {
            result.append("    include ").append(nodeRoot).append("/http/location/").append(stem)
                .append('/').append(locationFileName(location.getPath())).append(".conf;\n");
        }
        if (locations.stream().noneMatch(location -> "/".equals(location.getPath()))) {
            result.append("    location / { return 404; }\n");
        }
        return result.append("}\n").toString();
    }

    private String httpLocation(HttpServer server, HttpLocation location, HttpUpstream upstream, DnsResolverConfiguration resolver, String nodeRoot, Instant now) {
        return header("http.location." + server.getDomain() + "." + server.getListenPort() + "." + locationFileName(location.getPath()) + ".conf", now)
            + "# HTTP 接口转发规则，由控制面生成；修改请通过管理界面完成。\n"
            + "# service: " + server.getDomain() + "\n"
            + "# action: " + location.getPath() + "\n"
            + "# method: " + methods(location.getMethods()) + "\n"
            + "# upstream: " + (upstream == null ? "不适用" : upstream.getName()) + "\n"
            + "location " + location.getPath() + " {\n"
            + resolverDirective(resolver, "    ", false)
            + "    access_by_lua_block { require(\"runtime\").enforce() }\n"
            + rootDirective(location.getRootPath(), "    ")
            + (location.getAliasPath() == null || location.getAliasPath().isBlank() ? "" : "    alias " + directivePath(location.getAliasPath()) + ";\n")
            + responseHeaders(location.getResponseHeaders(), "    ")
            + rateLimit(location)
            + locationAction(location, upstream)
            + "    proxy_connect_timeout " + location.getProxyConnectTimeoutMs() + "ms;\n"
            + "    proxy_read_timeout " + location.getProxyReadTimeoutMs() + "ms;\n"
            + "    proxy_send_timeout " + location.getProxySendTimeoutMs() + "ms;\n"
            + "    proxy_set_header Host $host;\n"
            + "    proxy_set_header X-Real-IP $remote_addr;\n"
            + "    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n"
            + "    proxy_set_header X-Forwarded-Proto $scheme;\n"
            + "}\n";
    }

    private static String locationAction(HttpLocation location, HttpUpstream upstream) {
        if (location.getAction() == net.daoke.openrestyplus.httpconfig.LocationAction.RETURN) {
            String contentType = location.getReturnContentTypeMode() == net.daoke.openrestyplus.httpconfig.ReturnContentTypeMode.REQUEST ? "$http_content_type" : location.getReturnContentType();
            String type = contentType == null || contentType.isBlank() ? "text/plain" : contentType;
            String body = location.getReturnBody() == null ? "" : location.getReturnBody().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            return "    default_type " + type + ";\n    return " + location.getReturnStatus() + " \"" + body + "\";\n";
        }
        if (location.getAction() == net.daoke.openrestyplus.httpconfig.LocationAction.STATIC) return "    try_files $uri =404;\n";
        return dynamicDns(location, upstream);
    }

    private static String rootDirective(String value, String indent) { return value == null || value.isBlank() ? "" : indent + "root " + directivePath(value) + ";\n"; }
    private static String versionDirective(boolean hidden, String indent) { return hidden ? indent + "server_tokens off;\n" + indent + "more_clear_headers Server;\n" : ""; }
    private static String responseHeaders(List<String> headers, String indent) { StringBuilder result=new StringBuilder(); for(String header:headers){int split=header.indexOf(": ");if(split>0)result.append(indent).append("add_header ").append(header,0,split).append(" \"").append(header.substring(split+2).replace("\"","\\\"")).append("\" always;\n");}return result.toString(); }

    private static String rateZone(HttpLocation location) { return "openresty_plus_" + location.getId().toString().replace("-", ""); }

    private static String rateLimit(HttpLocation location) {
        if (!location.isRateLimitEnabled()) return "";
        String value = "    # 请求限速：" + location.getRatePerSecond() + " 次/秒。\n    limit_req zone=" + rateZone(location);
        if (location.getRateLimitBurst() > 0) value += " burst=" + location.getRateLimitBurst() + (location.isRateLimitNodelay() ? " nodelay" : "");
        return value + ";\n";
    }

    private static String dynamicDns(HttpLocation location, HttpUpstream upstream) {
        if (!location.isDynamicDnsEnabled()) return "    proxy_pass http://" + upstream.getName() + ";\n";
        if (location.getDynamicDnsHost() == null || location.getDynamicDnsPort() == null) throw invalid("Dynamic DNS location requires host and port");
        return "    # 目标域名赋值给变量；Resolver 缓存到期后会重新解析。\n"
            + "    set $openresty_plus_dynamic_target " + location.getDynamicDnsHost() + ";\n"
            + "    proxy_pass http://$openresty_plus_dynamic_target:" + location.getDynamicDnsPort() + ";\n";
    }

    private String streamUpstream(StreamUpstream upstream, Instant now) {
        return header("stream.upstream." + upstream.getName() + ".conf", now)
            + "# 四层上游服务：" + upstream.getName() + "。\n"
            + "upstream " + upstream.getName() + " {\n"
            + (upstream.isResolveEnabled()
                ? "    # 启用动态域名解析时，Upstream 必须使用共享内存保存运行时后端地址。\n"
                    + "    zone " + upstream.getName() + " " + upstream.getZoneSizeKilobytes() + "k;\n"
                : "")
            + "    server " + hostPort(upstream.getTargetHost(), upstream.getTargetPort()) + " max_fails=3 fail_timeout=10s"
            + (upstream.isResolveEnabled() ? " resolve" : "") + ";\n"
            + "}\n";
    }

    private String streamServer(StreamServer server, StreamUpstream upstream, DnsResolverConfiguration resolver, Instant now) {
        return header("stream.server." + server.getServiceName() + "." + server.getListenPort()
            + (server.getProtocol() == StreamProtocol.UDP ? ".udp" : "") + ".conf", now)
            + "# 四层监听服务：" + server.getServiceName() + ':' + server.getListenPort() + "。\n"
            + "# 协议：" + (server.getProtocol() == StreamProtocol.UDP ? "UDP" : "TCP") + "。\n"
            + "server {\n"
            + "    listen " + server.getListenPort() + (server.getProtocol() == StreamProtocol.UDP ? " udp" : "") + ";\n"
            + "    # 在读取 TCP/UDP 会话数据前校验本服务绑定的 IP 黑白名单。\n"
            + "    preread_by_lua_block { require(\"stream_runtime\").enforce() }\n"
            + resolverDirective(resolver, "    ", false)
            + streamProxyPass(server, upstream)
            + "    access_log " + directivePath(server.getAccessLog()) + " openresty_plus_stream;\n"
            + "    error_log " + directivePath(server.getErrorLog()) + " warn;\n"
            + "}\n";
    }

    private static String streamProxyPass(StreamServer server, StreamUpstream upstream) {
        if (!server.isDynamicDnsEnabled()) return "    proxy_pass " + upstream.getName() + ";\n";
        if (server.getDynamicDnsHost() == null || server.getDynamicDnsPort() == null) throw invalid("Dynamic DNS stream server requires host and port");
        requireHostOrAddress(server.getDynamicDnsHost(), "Stream dynamic DNS host");
        return "    # 通过 stream 级 map 变量引用域名；Resolver 缓存到期后会重新解析。\n"
            + "    proxy_pass $openresty_plus_stream_target:" + server.getDynamicDnsPort() + ";\n";
    }

    private static DnsResolverConfiguration resolver(List<DnsResolverConfiguration> values, DnsResolverScope scope, UUID targetId) {
        return values.stream().filter(value -> value.getScope() == scope && java.util.Objects.equals(value.getTargetResourceId(), targetId)).findFirst().orElse(null);
    }

    private static String resolverDirective(DnsResolverConfiguration resolver, String indent, boolean dockerFallback) {
        if (resolver == null) return dockerFallback ? indent + "resolver 127.0.0.11 ipv6=off valid=10s;\n" : "";
        return indent + "# DNS Resolver：缓存 " + resolver.getValidSeconds() + " 秒。\n"
            + indent + "resolver " + String.join(" ", resolver.getResolverAddresses()) + " valid=" + resolver.getValidSeconds() + "s ipv6=" + (resolver.isIpv6Enabled() ? "on" : "off") + ";\n"
            + indent + "resolver_timeout " + resolver.getTimeoutMilliseconds() + "ms;\n";
    }

    private String header(String id, Instant now) {
        return "# config_id: " + id + "\n# version: database-rendered\n# updated: " + METADATA_TIME.format(now)
            + "\n# author: admin\n\n";
    }

    private static <T> Map<UUID, T> index(List<T> values) {
        Map<UUID, T> indexed = new LinkedHashMap<>();
        for (T value : values) {
            UUID id = value instanceof HttpUpstream upstream ? upstream.getId() : ((StreamUpstream) value).getId();
            indexed.put(id, value);
        }
        return indexed;
    }

    private static void requireName(String value, String field) {
        if (value == null || !value.matches("[a-z0-9][a-z0-9.-]{0,127}")) {
            throw invalid(field + " must contain lower-case letters, digits, dots or hyphens only");
        }
    }

    private static void requireHost(String value, String field) {
        if (value == null || !value.matches("[a-z0-9][a-z0-9.-]{0,253}")) {
            throw invalid(field + " is invalid");
        }
    }

    private static void requireHostOrAddress(String value, String field) {
        if (value == null || !value.matches("[0-9a-zA-Z:.\\-]{1,253}")) {
            throw invalid(field + " is invalid");
        }
    }

    private static void requireLocationPath(String path) {
        if (path == null || !path.matches("/[A-Za-z0-9._~/%=&-]*") || path.contains("..")) {
            throw invalid("HTTP location path is invalid");
        }
    }

    private static String locationFileName(String path) {
        return "/".equals(path) ? "_default" : path.substring(1).replace('/', '.').replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String methods(List<String> values) {
        if (values == null || values.isEmpty() || values.stream().anyMatch(value -> value == null || !value.matches("[A-Z]{1,16}"))) {
            throw invalid("HTTP location method is invalid");
        }
        return String.join(",", values);
    }

    private static String directivePath(String path) {
        if (path == null || !path.matches("/[A-Za-z0-9._/-]{1,511}")) {
            throw invalid("Log path is invalid");
        }
        return path;
    }

    private static String hostPort(String host, int port) {
        return host.contains(":") ? "[" + host + "]:" + port : host + ":" + port;
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private static String checksum(Map<String, String> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
            });
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var files = Files.walk(root)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    public record RenderedConfiguration(UUID centerId, String centerCode, Instant generatedAt, String nodeEntryPoint,
                                        Map<String, String> files, List<String> warnings, String checksum) { }
    public record MaterializedConfiguration(RenderedConfiguration configuration, Path directory) { }
}
