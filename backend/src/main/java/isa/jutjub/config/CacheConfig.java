package isa.jutjub.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import isa.jutjub.model.VideoPost;
import isa.jutjub.repository.VideoPostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration for L2 Caching with Ehcache
 * 
 * L2 Cache Configuration:
 * - Enabled via application.properties
 * - Entity caching via @Cacheable annotations on model classes
 * - Cache regions defined in ehcache.xml
 * - Statistics enabled for monitoring
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    private final VideoPostRepository videoPostRepository;

    public CacheConfig(VideoPostRepository videoPostRepository) {
        this.videoPostRepository = videoPostRepository;
    }

    /**
     * Caffeine config for LoadingCache with auto-refresh capability
     */
    private Caffeine<Object, Object> getLoadingCacheConfig() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .refreshAfterWrite(3, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * LoadingCache for individual video posts with circuit breaker protection
     */
    @Bean
    public LoadingCache<Long, VideoPost> videoPostLoadingCache(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("videoPostCache");

        return getLoadingCacheConfig()
                .build(key -> {
                    log.debug("L3 cache miss for video post {}, checking L2/database", key);
                    return circuitBreaker.executeSupplier(() ->
                            videoPostRepository.findById(key)
                                    .orElseThrow(() -> new EntityNotFoundException("Video post not found: " + key))
                    );
                });
    }

    /**
     * CacheManager for Spring caching
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());

        cacheManager.setCacheNames(List.of(
                "users",
                "comments",
                "commentCounts",
                "likeCounts",
                "thumbnails",
                "videoPosts",
                "videoPostsPage",
                "recentVideoPosts",
                "popularVideoPosts",
                "videoSearch",
                "videoPostsByTag",
                "videoPostsAfterDate",
                "videoPostsBeforeDate",
                "videoPostsDateRange",
                "tiles"
        ));
        return cacheManager;
    }

    /**
     * Circuit breaker configuration for database operations
     * Opens after 50% failure rate with minimum 5 calls
     * Stays open for 30 seconds before attempting recovery
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit at 50% failure rate
                .slowCallRateThreshold(50) // Consider slow calls as failures
                .slowCallDurationThreshold(Duration.ofSeconds(2)) // Calls taking >2s are "slow"
                .minimumNumberOfCalls(5) // Need at least 5 calls before calculating failure rate
                .slidingWindowSize(10) // Track last 10 calls
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Stay open for 30s
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 test calls in half-open state
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class) // Record all exceptions
                .ignoreExceptions(EntityNotFoundException.class) // Don't count "not found" as failures
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // Add event listeners for monitoring
        registry.circuitBreaker("videoPostCache").getEventPublisher()
                .onStateTransition(event ->
                        log.warn("Circuit breaker state transition: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onError(event ->
                        log.error("Circuit breaker recorded error: {}", event.getThrowable().getMessage()))
                .onSuccess(event ->
                        log.debug("Circuit breaker recorded success"));

        return registry;
    }


    /**
     * Custom executor for async cache operations
     * Prevents blocking the common ForkJoinPool
     */
    @Bean(name = "cacheAsyncExecutor")
    public Executor cacheAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cache-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}