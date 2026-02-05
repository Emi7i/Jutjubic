package isa.jutjub.controller;

import isa.jutjub.model.VideoPost;
import isa.jutjub.service.VideoPostCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Example controller showing how to use the VideoPostCacheService
 * with circuit breaker protection
 */
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoCacheController {

    private final VideoPostCacheService cacheService;

    /**
     * Get video post - throws exception if circuit is open
     */
    @GetMapping("/{id}")
    public ResponseEntity<VideoPost> getVideoPost(@PathVariable Long id) {
        VideoPost video = cacheService.getVideoPost(id);
        return ResponseEntity.ok(video);
    }

    /**
     * Get video post with graceful degradation
     * Returns 503 if circuit is open instead of throwing exception
     */
    @GetMapping("/{id}/safe")
    public ResponseEntity<VideoPost> getVideoPostSafe(@PathVariable Long id) {
        Optional<VideoPost> video = cacheService.getVideoPostSafe(id);
        return video
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(503).build());
    }

    /**
     * Get video post asynchronously
     */
    @GetMapping("/{id}/async")
    public CompletableFuture<ResponseEntity<VideoPost>> getVideoPostAsync(@PathVariable Long id) {
        return cacheService.getVideoPostAsync(id)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(503).build());
    }

    /**
     * Invalidate cache for a video
     */
    @DeleteMapping("/{id}/cache")
    public ResponseEntity<Void> invalidateCache(@PathVariable Long id) {
        cacheService.invalidateVideoPost(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Refresh cache for a video (async reload)
     */
    @PostMapping("/{id}/cache/refresh")
    public ResponseEntity<Void> refreshCache(@PathVariable Long id) {
        cacheService.refreshVideoPost(id);
        return ResponseEntity.accepted().build();
    }

    /**
     * Clear all cache
     */
    @DeleteMapping("/cache/all")
    public ResponseEntity<Void> clearAllCache() {
        cacheService.clearAllCache();
        return ResponseEntity.noContent().build();
    }

    /**
     * Get cache statistics
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<String> getCacheStats() {
        return ResponseEntity.ok(cacheService.getFullStats());
    }

    /**
     * Check circuit breaker status
     */
    @GetMapping("/circuit-breaker/status")
    public ResponseEntity<CircuitBreakerStatus> getCircuitBreakerStatus() {
        return ResponseEntity.ok(new CircuitBreakerStatus(
                cacheService.isCircuitBreakerOpen(),
                cacheService.getCircuitBreakerStats()
        ));
    }

    /**
     * Manually reset circuit breaker (admin only)
     */
    @PostMapping("/circuit-breaker/reset")
    public ResponseEntity<Void> resetCircuitBreaker() {
        cacheService.resetCircuitBreaker();
        return ResponseEntity.ok().build();
    }

    record CircuitBreakerStatus(boolean isOpen, String stats) {}
}
