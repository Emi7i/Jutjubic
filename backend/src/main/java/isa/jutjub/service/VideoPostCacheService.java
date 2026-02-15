package isa.jutjub.service;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import isa.jutjub.model.Videos;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class VideoPostCacheService {

    private final LoadingCache<Long, Videos> videoPostLoadingCache;
    private final CircuitBreaker circuitBreaker;
    private final Executor cacheAsyncExecutor;

    public VideoPostCacheService(
            LoadingCache<Long, Videos> videoPostLoadingCache,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Qualifier("cacheAsyncExecutor") Executor cacheAsyncExecutor) {
        this.videoPostLoadingCache = videoPostLoadingCache;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("videoPostCache");
        this.cacheAsyncExecutor = cacheAsyncExecutor;
    }

    /**
     * Gets a video post with cache stampede protection and circuit breaker
     * Uses Caffeine's refreshAfterWrite for entries accessed after 3 minutes
     *
     * @param id the video post ID
     * @return the video post
     * @throws EntityNotFoundException if video post not found
     * @throws CallNotPermittedException if circuit breaker is open
     */
    public Videos getVideoPost(Long id) {
        log.debug("Getting video post {} with cache protection and circuit breaker", id);
        return videoPostLoadingCache.get(id);
    }

    /**
     * Gets a video post with fallback when circuit breaker is open
     * Returns Optional.empty() if circuit is open or video not found
     *
     * @param id the video post ID
     * @return Optional containing the video post, or empty if unavailable
     */
    public Optional<Videos> getVideoPostSafe(Long id) {
        log.debug("Getting video post {} with safe fallback", id);

        try {
            return Optional.of(videoPostLoadingCache.get(id));
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker is OPEN for video post {}, returning empty", id);
            return Optional.empty();
        } catch (EntityNotFoundException e) {
            log.debug("Video post {} not found", id);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error getting video post {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets a video post asynchronously with circuit breaker protection
     * Uses dedicated executor to avoid blocking common pool
     *
     * @param id the video post ID
     * @return CompletableFuture containing the video post
     */
    public CompletableFuture<Videos> getVideoPostAsync(Long id) {
        log.debug("Getting video post {} asynchronously with circuit breaker", id);
        return CompletableFuture.supplyAsync(() -> getVideoPost(id), cacheAsyncExecutor);
    }

    /**
     * Gets a video post asynchronously with safe fallback
     *
     * @param id the video post ID
     * @return CompletableFuture containing Optional of video post
     */
    public CompletableFuture<Optional<Videos>> getVideoPostAsyncSafe(Long id) {
        log.debug("Getting video post {} asynchronously with safe fallback", id);
        return CompletableFuture.supplyAsync(() -> getVideoPostSafe(id), cacheAsyncExecutor);
    }

    /**
     * Invalidates cache for a specific video post
     * This will force the next request to reload from database
     *
     * @param id the video post ID
     */
    public void invalidateVideoPost(Long id) {
        log.debug("Invalidating cache for video post {}", id);
        videoPostLoadingCache.invalidate(id);
    }

    /**
     * Gets cache statistics with performance information
     *
     * @return cache statistics including hit rates and load times
     */
    public String getCacheStats() {
        var stats = videoPostLoadingCache.stats();
        return String.format(
                "Cache Stats - Hit Rate: %.2f%%, Miss Rate: %.2f%%, Size: %d, Requests: %d, Evictions: %d, Load Time: %.2fμs",
                stats.hitRate() * 100,
                stats.missRate() * 100,
                videoPostLoadingCache.estimatedSize(),
                stats.requestCount(),
                stats.evictionCount(),
                stats.averageLoadPenalty() / 1000 // Convert nanoseconds to microseconds
        );
    }

    /**
     * Gets circuit breaker statistics
     *
     * @return circuit breaker state and metrics
     */
    public String getCircuitBreakerStats() {
        var metrics = circuitBreaker.getMetrics();
        return String.format(
                "Circuit Breaker - State: %s, Failure Rate: %.2f%%, Slow Call Rate: %.2f%%, Buffered Calls: %d, Failed Calls: %d",
                circuitBreaker.getState(),
                metrics.getFailureRate(),
                metrics.getSlowCallRate(),
                metrics.getNumberOfBufferedCalls(),
                metrics.getNumberOfFailedCalls()
        );
    }

    /**
     * Checks if circuit breaker is currently open
     *
     * @return true if circuit is open (blocking calls)
     */
    public boolean isCircuitBreakerOpen() {
        return circuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    /**
     * Manually transition circuit breaker to closed state
     * Use with caution - only when you're certain the issue is resolved
     */
    public void resetCircuitBreaker() {
        log.warn("Manually resetting circuit breaker to CLOSED state");
        circuitBreaker.transitionToClosedState();
    }

    /**
     * Refreshes a specific video post in cache
     * This will asynchronously reload the data
     *
     * @param id the video post ID
     */
    public void refreshVideoPost(Long id) {
        log.debug("Refreshing cache for video post {}", id);
        videoPostLoadingCache.refresh(id);
    }

    /**
     * Clears all video posts from cache
     */
    public void clearAllCache() {
        log.info("Clearing all video post cache");
        videoPostLoadingCache.invalidateAll();
    }

    /**
     * Gets combined statistics report
     *
     * @return formatted statistics including cache and circuit breaker metrics
     */
    public String getFullStats() {
        return String.format("%s%n%s", getCacheStats(), getCircuitBreakerStats());
    }
}