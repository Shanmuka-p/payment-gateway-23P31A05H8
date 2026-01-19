package com.gateway.api;

import com.gateway.queue.RedisQueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test/jobs/status")
public class JobStatusController {
    private final RedisQueueService queueService;

    public JobStatusController(RedisQueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("pending", queueService.pendingCount());
        resp.put("processing", queueService.getCounter("metrics:jobs:processing"));
        resp.put("completed", queueService.getCounter("metrics:jobs:completed"));
        resp.put("failed", queueService.getCounter("metrics:jobs:failed"));
        resp.put("worker_status", queueService.getWorkerStatus());
        return ResponseEntity.ok(resp);
    }
}
