package com.gateway.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class RedisQueueService {
    public static final String QUEUE_PROCESS_PAYMENT = "queue:payment.process";
    public static final String QUEUE_DELIVER_WEBHOOK = "queue:webhook.deliver";
    public static final String QUEUE_PROCESS_REFUND = "queue:refund.process";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisQueueService(StringRedisTemplate redis) {
        this.redis = redis;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    public void enqueue(String queue, Map<String, Object> payload) {
        try {
            String s = mapper.writeValueAsString(payload);
            redis.opsForList().leftPush(queue, s);
            redis.opsForValue().increment("metrics:jobs:pending");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String blockingPop(String queue, Duration timeout) {
        return redis.opsForList().rightPop(queue, timeout);
    }

    public long pendingCount() {
        Long a = redis.opsForList().size(QUEUE_PROCESS_PAYMENT);
        Long b = redis.opsForList().size(QUEUE_DELIVER_WEBHOOK);
        Long c = redis.opsForList().size(QUEUE_PROCESS_REFUND);
        return (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c);
    }

    public void incrProcessing() { redis.opsForValue().increment("metrics:jobs:processing"); }
    public void decrProcessing() { redis.opsForValue().decrement("metrics:jobs:processing"); }
    public void incrCompleted() { redis.opsForValue().increment("metrics:jobs:completed"); }
    public void incrFailed() { redis.opsForValue().increment("metrics:jobs:failed"); }

    public long getCounter(String key) {
        String v = redis.opsForValue().get(key);
        if (v == null) return 0L;
        try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; }
    }

    public void setWorkerHeartbeat() {
        redis.opsForValue().set("worker:status", "1", Duration.ofSeconds(15));
    }

    public String getWorkerStatus() {
        String v = redis.opsForValue().get("worker:status");
        return v != null ? "running" : "stopped";
    }
}
