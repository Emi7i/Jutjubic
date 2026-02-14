package isa.jutjub.service.rate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
@Slf4j
public class SlidingWindowRateLimiter {

    @Autowired
    public RedisTemplate<String, Object> redisTemplate;

    private static final String SLIDING_WINDOW_SCRIPT = """
        local key = KEYS[1]
        local window_size_ms = tonumber(ARGV[1])
        local max_requests = tonumber(ARGV[2])
        local current_time = tonumber(ARGV[3])
        local request_weight = tonumber(ARGV[4])
        
        local window_start = current_time - window_size_ms
        
        redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
        
        local current_requests = redis.call('ZCARD', key)
        
        if current_requests + request_weight <= max_requests then
            for i = 1, request_weight do
                redis.call('ZADD', key, current_time, current_time + i)
            end
            redis.call('EXPIRE', key, math.ceil(window_size_ms / 1000) + 1)
            return 1
        else
            redis.call('EXPIRE', key, math.ceil(window_size_ms / 1000) + 1)
            return 0
        end
        """;

    public boolean tryConsume(String key, int requestWeight, long maxRequests, Duration windowSize) {
        long windowSizeMs = windowSize.toMillis();
        long currentTime = System.currentTimeMillis();
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(SLIDING_WINDOW_SCRIPT, Long.class);
        
        Long result = redisTemplate.execute(script,
            Collections.singletonList(key),
            String.valueOf(windowSizeMs),
            String.valueOf(maxRequests),
            String.valueOf(currentTime),
            String.valueOf(requestWeight)
        );
        
        boolean consumed = result != null && result == 1;
        if (!consumed) {
            log.debug("Sliding window rate limit exceeded for key: {}, window: {}ms, max: {}, current: {}", 
                     key, windowSizeMs, maxRequests, getCurrentRequestCount(key, windowSizeMs, currentTime));
        }
        return consumed;
    }

    public long getCurrentRequestCount(String key, long windowSizeMs, long currentTime) {
        long windowStart = currentTime - windowSizeMs;
        Long count = redisTemplate.opsForZSet().count(key, windowStart, currentTime);
        return count != null ? count : 0;
    }

    public boolean tryConsume(String key, long maxRequests, Duration windowSize) {
        return tryConsume(key, 1, maxRequests, windowSize);
    }

    public void resetWindow(String key) {
        redisTemplate.delete(key);
        log.info("Reset sliding window for key: {}", key);
    }
}
