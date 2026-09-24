package net.daoke.openrestyplus.httpconfig;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.net.InetSocketAddress;
import java.util.List;

/** Runs an on-demand HTTP probe using the health-check settings owned by an upstream. */
@Service
public class HttpUpstreamHealthCheckService {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public List<Result> check(HttpUpstream upstream, List<HttpUpstreamTarget> targets) {
        if (!upstream.isHealthCheckEnabled()) return targets.stream().map(target -> new Result(target.getId(), target.getTargetHost(), target.getTargetPort(), "NOT_CONFIGURED", null, "未启用主动健康检查")).toList();
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            return executor.invokeAll(targets.stream().filter(HttpUpstreamTarget::isEnabled).<java.util.concurrent.Callable<Result>>map(target -> () -> checkOne(upstream, target)).toList())
                .stream().map(future -> { try { return future.get(); } catch (Exception exception) { return new Result(null, "", 0, "FAILED", null, "健康检查执行失败"); } }).toList();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    private Result checkOne(HttpUpstream upstream, HttpUpstreamTarget target) {
        try {
            if (upstream.getHealthCheckType() != net.daoke.openrestyplus.health.HealthCheckType.HTTP) {
                try (var socket = new java.net.Socket()) { socket.connect(new InetSocketAddress(target.getTargetHost(), target.getTargetPort()), upstream.getHealthCheckTimeoutMilliseconds()); }
                return new Result(target.getId(), target.getTargetHost(), target.getTargetPort(), "HEALTHY", null, upstream.getHealthCheckType() == net.daoke.openrestyplus.health.HealthCheckType.PING ? "PING（TCP 连接）成功" : "TCP 连接成功");
            }
            String host = target.getTargetHost().contains(":") ? "[" + target.getTargetHost() + "]" : target.getTargetHost();
            URI uri = URI.create("http://" + host + ":" + target.getTargetPort() + upstream.getHealthCheckPath());
            var builder = HttpRequest.newBuilder(uri).GET().timeout(Duration.ofMillis(upstream.getHealthCheckTimeoutMilliseconds()));
            if (upstream.getHealthCheckHost() != null && !upstream.getHealthCheckHost().isBlank()) builder.header("Host", upstream.getHealthCheckHost());
            for (String header : upstream.getHealthCheckRequestHeaders()) { int index = header.indexOf(':'); if (index > 0) builder.header(header.substring(0,index).trim(), header.substring(index+1).trim()); }
            var request = builder.build();
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            boolean ok = response.statusCode() == upstream.getHealthCheckExpectedStatus();
            return new Result(target.getId(), target.getTargetHost(), target.getTargetPort(), ok ? "HEALTHY" : "UNHEALTHY", response.statusCode(), ok ? "状态码符合预期" : "状态码与预期不符");
        } catch (Exception exception) {
            return new Result(target.getId(), target.getTargetHost(), target.getTargetPort(), "UNHEALTHY", null, exception.getMessage() == null ? "连接失败" : exception.getMessage());
        }
    }

    public record Result(java.util.UUID targetId, String targetHost, int targetPort, String status, Integer httpStatus, String message) { }
}
