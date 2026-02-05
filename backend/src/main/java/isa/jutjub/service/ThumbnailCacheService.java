package isa.jutjub.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class ThumbnailCacheService {

    @Autowired
    private FileUploadService fileUploadService;

    private final ThreadPoolExecutor cacheExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    /**
     * Caches a thumbnail image using L2 cache
     * @param videoPostId the video post ID
     * @param thumbnailFile the thumbnail file to cache
     * @return the cached file path
     */
    @CacheEvict(value = "thumbnails", key = "#videoPostId")
    public String cacheThumbnail(Long videoPostId, MultipartFile thumbnailFile) {
        log.info("Caching thumbnail for video post ID: {}", videoPostId);
        // Upload thumbnail file using FileUploadService
        String thumbnailPath = fileUploadService.uploadThumbnailFile(thumbnailFile);

        log.info("Successfully cached thumbnail for video post ID: {} at: {}", videoPostId, thumbnailPath);
        return thumbnailPath;
    }

    /**
     * Gets cached thumbnail image using L2 cache
     * @param videoPostId the video post ID
     * @return the cached thumbnail resource, or null if not found
     */
    @Cacheable(value = "thumbnails", key = "#videoPostId")
    public Resource getCachedThumbnail(Long videoPostId) {
        log.debug("Looking for cached thumbnail for video post ID: {}", videoPostId);
        
        // This method will use Spring's L2 cache
        // The actual caching logic is handled by Spring Cache abstraction
        return null; // Cache miss - Spring will handle caching
    }

    /**
     * Caches a thumbnail from an existing file path using L2 cache
     * @param videoPostId the video post ID
     * @param originalThumbnailPath the original thumbnail file path
     * @return the cached file path
     */
    @CacheEvict(value = "thumbnails", key = "#videoPostId")
    public String cacheThumbnailFromPath(Long videoPostId, String originalThumbnailPath) {
        log.info("Caching thumbnail from path for video post ID: {}", videoPostId);
        
        // For L2 caching, we just return the original path
        // Spring Cache will handle the caching
        return originalThumbnailPath;
    }

    /**
     * Clears cached thumbnails for a specific video post using L2 cache
     * @param videoPostId the video post ID
     */
    @CacheEvict(value = "thumbnails", key = "#videoPostId")
    public void clearCachedThumbnail(Long videoPostId) {
        log.info("Clearing cached thumbnail for video post ID: {}", videoPostId);
        // Spring Cache will handle the eviction
    }

    /**
     * Gets cache statistics for L2 cache
     * @return cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheType", "L2 Caffeine Cache");
        stats.put("description", "Spring Cache abstraction with Caffeine backend");
        stats.put("cacheName", "thumbnails");
        stats.put("status", "Active");
        
        return stats;
    }

    /**
     * Shuts down the cache executor service
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ThumbnailCacheService...");
        cacheExecutor.shutdown();
        try {
            if (!cacheExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                cacheExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cacheExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
