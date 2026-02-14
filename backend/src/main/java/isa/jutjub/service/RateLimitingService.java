package isa.jutjub.service;

import isa.jutjub.service.rate.SlidingWindowRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

@Service
@Slf4j
public class RateLimitingService {

    @Autowired
    private SlidingWindowRateLimiter slidingWindowRateLimiter;

    public enum RateLimitType {
        VIDEO_UPLOAD("video_upload", 10, Duration.ofMinutes(1)),
        VIDEO_SEARCH("video_search", 100, Duration.ofMinutes(1)),
        VIDEO_VIEW("video_view", 50, Duration.ofMinutes(1)),
        VIDEO_LIKE("video_like", 20, Duration.ofMinutes(1)),
        GENERAL_API("general_api", 100, Duration.ofMinutes(1));

        private final String keyPrefix;
        private final long maxRequests;
        private final Duration windowSize;

        RateLimitType(String keyPrefix, long maxRequests, Duration windowSize) {
            this.keyPrefix = keyPrefix;
            this.maxRequests = maxRequests;
            this.windowSize = windowSize;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public long getMaxRequests() {
            return maxRequests;
        }

        public Duration getWindowSize() {
            return windowSize;
        }
    }

    public boolean isAllowed(String identifier, RateLimitType rateLimitType) {
        String key = rateLimitType.getKeyPrefix() + ":" + identifier;
        boolean allowed = slidingWindowRateLimiter.tryConsume(
            key, 
            rateLimitType.getMaxRequests(), 
            rateLimitType.getWindowSize()
        );
        
        if (!allowed) {
            log.warn("Rate limit exceeded for identifier: {}, type: {}", identifier, rateLimitType);
        }
        
        return allowed;
    }

    public boolean isAllowed(String identifier, RateLimitType rateLimitType, int requestWeight) {
        String key = rateLimitType.getKeyPrefix() + ":" + identifier;
        boolean allowed = slidingWindowRateLimiter.tryConsume(
            key, 
            requestWeight,
            rateLimitType.getMaxRequests(), 
            rateLimitType.getWindowSize()
        );
        
        if (!allowed) {
            log.warn("Rate limit exceeded for identifier: {}, type: {}, weight: {}", 
                    identifier, rateLimitType, requestWeight);
        }
        
        return allowed;
    }

    public String getClientIdentifier(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    public void resetRateLimit(String identifier, RateLimitType rateLimitType) {
        String key = rateLimitType.getKeyPrefix() + ":" + identifier;
        slidingWindowRateLimiter.resetWindow(key);
        log.info("Reset rate limit for identifier: {}, type: {}", identifier, rateLimitType);
    }
}
