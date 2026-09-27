package net.daoke.openrestyplus.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OpenrestyLogStream {
    private static final int MAX_LINES = 2000;
    private final ObjectMapper mapper;
    private final Map<String, ArrayDeque<LogEvent>> buffers = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, SseEmitter>> emitters = new ConcurrentHashMap<>();

    public OpenrestyLogStream() { this.mapper = new ObjectMapper(); }

    @KafkaListener(topics = "${openresty.logs.kafka-topic:dtl-602-openresty-plus}")
    public void onMessage(String payload) {
        try {
            var json = mapper.readTree(payload);
            var node = text(json.at("/openresty/node_id"));
            var nodeName = text(json.at("/openresty/node_name"));
            var key = !node.isBlank() ? node : nodeName;
            if (key.isBlank()) return;
            var event = new LogEvent(node, nodeName, text(json.get("openresty_log_type")),
                text(json.at("/log/file/path")), text(json.get("message")),
                text(json.get("@timestamp")), Instant.now().toEpochMilli());
            var queue = buffers.computeIfAbsent(key, ignored -> new ArrayDeque<>());
            synchronized (queue) {
                queue.addLast(event);
                while (queue.size() > MAX_LINES) queue.removeFirst();
            }
            var targets = emitters.get(key);
            if (targets != null) targets.entrySet().removeIf(entry -> !send(entry.getKey(), entry.getValue(), event));
        } catch (Exception ignored) {
            // A malformed third party event must not stop the Kafka consumer.
        }
    }

    public SseEmitter subscribe(String key) {
        var emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>()).put(UUID.randomUUID(), emitter);
        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> remove(key, emitter));
        try { emitter.send(SseEmitter.event().name("ready").data(Map.of("node", key))); } catch (IOException exception) { remove(key, emitter); }
        return emitter;
    }

    public String resolveKey(String nodeId, String nodeName) {
        if (buffers.containsKey(nodeId)) return nodeId;
        return nodeName;
    }

    private boolean send(UUID id, SseEmitter emitter, LogEvent event) {
        try { emitter.send(SseEmitter.event().name("log").data(event)); return true; }
        catch (IOException exception) { emitter.complete(); return false; }
    }

    private void remove(String key, SseEmitter emitter) { emitters.getOrDefault(key, Map.of()).values().removeIf(value -> value == emitter); }
    private static String text(JsonNode node) { return node == null || node.isMissingNode() || node.isNull() ? "" : node.asText(""); }

    public record LogEvent(String nodeId, String nodeName, String type, String path, String message, String timestamp, long receivedAt) { }
}
