package net.daoke.openrestyplus.task;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class TaskStream {
    public static final String STREAM = "openresty-plus:tasks";
    public static final String GROUP = "control-plane";
    private final RedisTemplate<String, String> redis;
    public TaskStream(@Qualifier("taskStreamRedisTemplate") RedisTemplate<String, String> redis) { this.redis = redis; }
    public String enqueue(String taskId, String taskType, String centerId, String artifactId) {
        var record = MapRecord.create(STREAM, Map.of("taskId", taskId, "taskType", taskType, "centerId", centerId, "artifactId", artifactId));
        return redis.opsForStream().add(record).getValue();
    }
    public void ensureGroup() {
        try { redis.opsForStream().createGroup(STREAM, ReadOffset.from("0-0"), GROUP); }
        catch (Exception ignored) { }
    }
}
