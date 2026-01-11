package isa.jutjub.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.cache.thumbnail.dir:./cache/thumbnails}")
    private String cacheDir;

    @Value("${app.cache.thumbnail.max-size-mb:100}")
    private int maxCacheSizeMB;

    @Value("${app.cache.thumbnail.cleanup-interval-hours:24}")
    private int cleanupIntervalHours;

    private final ThreadPoolExecutor cacheExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    /**
     * Caches a thumbnail image for faster access
     * @param videoPostId the video post ID
     * @param thumbnailFile the thumbnail file to cache
     * @return the cached file path
     */
    public String cacheThumbnail(Long videoPostId, MultipartFile thumbnailFile) {
        log.info("Caching thumbnail for video post ID: {}", videoPostId);
        
        try {
            // Create cache directory if it doesn't exist
            Path cachePath = Paths.get(cacheDir);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }

            // Generate cache filename
            String cacheFilename = "thumbnail_" + videoPostId + "_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".jpg";
            Path cacheFilePath = cachePath.resolve(cacheFilename);

            // Copy file to cache
            Files.copy(thumbnailFile.getInputStream(), cacheFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Verify file was cached correctly
            if (!Files.exists(cacheFilePath) || Files.size(cacheFilePath) == 0) {
                throw new RuntimeException("Thumbnail cache verification failed");
            }
            
            log.info("Successfully cached thumbnail for video post ID: {} at: {}", videoPostId, cacheFilePath);
            return cacheFilePath.toString();

        } catch (IOException e) {
            log.error("Failed to cache thumbnail for video post ID {}: {}", videoPostId, e.getMessage());
            throw new RuntimeException("Failed to cache thumbnail: " + e.getMessage(), e);
        }
    }

    /**
     * Gets cached thumbnail image
     * @param videoPostId the video post ID
     * @return the cached thumbnail resource, or null if not found
     */
    public Resource getCachedThumbnail(Long videoPostId) {
        try {
            Path cachePath = Paths.get(cacheDir);
            
            // Find the most recent cached thumbnail for this video post
            Path cachedFile = Files.list(cachePath)
                .filter(path -> path.getFileName().toString().startsWith("thumbnail_" + videoPostId + "_"))
                .filter(path -> path.getFileName().toString().endsWith(".jpg"))
                .max((p1, p2) -> {
                    // Compare by timestamp in filename
                    String name1 = p1.getFileName().toString();
                    String name2 = p2.getFileName().toString();
                    return name1.compareTo(name2);
                })
                .orElse(null);

            if (cachedFile != null && Files.exists(cachedFile) && Files.isReadable(cachedFile)) {
                log.debug("Found cached thumbnail for video post ID: {}", videoPostId);
                return new UrlResource(cachedFile.toUri());
            }

            log.debug("No cached thumbnail found for video post ID: {}", videoPostId);
            return null;

        } catch (Exception e) {
            log.error("Failed to get cached thumbnail for video post ID {}: {}", videoPostId, e.getMessage());
            return null;
        }
    }

    /**
     * Caches a thumbnail from an existing file path
     * @param videoPostId the video post ID
     * @param originalThumbnailPath the original thumbnail file path
     * @return the cached file path
     */
    public String cacheThumbnailFromPath(Long videoPostId, String originalThumbnailPath) {
        try {
            Path originalPath = Paths.get(originalThumbnailPath);
            if (!Files.exists(originalPath)) {
                throw new RuntimeException("Original thumbnail file not found: " + originalThumbnailPath);
            }

            // Create cache directory if it doesn't exist
            Path cachePath = Paths.get(cacheDir);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }

            // Generate cache filename
            String cacheFilename = "thumbnail_" + videoPostId + "_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".jpg";
            Path cacheFilePath = cachePath.resolve(cacheFilename);

            // Copy file to cache
            Files.copy(originalPath, cacheFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("Successfully cached thumbnail from path for video post ID: {} at: {}", videoPostId, cacheFilePath);
            return cacheFilePath.toString();

        } catch (IOException e) {
            log.error("Failed to cache thumbnail from path for video post ID {}: {}", videoPostId, e.getMessage());
            throw new RuntimeException("Failed to cache thumbnail from path: " + e.getMessage(), e);
        }
    }

    /**
     * Clears cached thumbnails for a specific video post
     * @param videoPostId the video post ID
     */
    public void clearCachedThumbnail(Long videoPostId) {
        try {
            Path cachePath = Paths.get(cacheDir);
            
            Files.list(cachePath)
                .filter(path -> path.getFileName().toString().startsWith("thumbnail_" + videoPostId + "_"))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        log.info("Deleted cached thumbnail: {}", path);
                    } catch (IOException e) {
                        log.error("Failed to delete cached thumbnail {}: {}", path, e.getMessage());
                    }
                });

        } catch (IOException e) {
            log.error("Failed to clear cached thumbnails for video post ID {}: {}", videoPostId, e.getMessage());
        }
    }

    /**
     * Cleans up old cached thumbnails based on cache size and age
     */
    public void cleanupCache() {
        log.info("Starting thumbnail cache cleanup...");
        
        try {
            Path cachePath = Paths.get(cacheDir);
            if (!Files.exists(cachePath)) {
                return;
            }

            // Get all cached files with their metadata
            Map<Path, Long> cachedFiles = new HashMap<>();
            Files.list(cachePath)
                .filter(path -> path.getFileName().toString().startsWith("thumbnail_") && 
                              path.getFileName().toString().endsWith(".jpg"))
                .forEach(path -> {
                    try {
                        cachedFiles.put(path, Files.getLastModifiedTime(path).toMillis());
                    } catch (IOException e) {
                        log.error("Failed to get last modified time for {}: {}", path, e.getMessage());
                    }
                });

            // If cache exceeds size limit, remove oldest files
            long totalSize = cachedFiles.keySet().stream()
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();

            long maxSizeBytes = maxCacheSizeMB * 1024L * 1024L;
            if (totalSize > maxSizeBytes) {
                log.info("Cache size {}MB exceeds limit {}MB, cleaning up...", 
                    totalSize / (1024 * 1024), maxCacheSizeMB);

                // Sort by last modified time (oldest first)
                final long[] currentSize = {totalSize};
                cachedFiles.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(entry -> {
                        if (currentSize[0] > maxSizeBytes) {
                            try {
                                long fileSize = Files.size(entry.getKey());
                                Files.delete(entry.getKey());
                                currentSize[0] -= fileSize;
                                log.debug("Deleted old cached file: {}", entry.getKey());
                            } catch (IOException e) {
                                log.error("Failed to delete old cached file {}: {}", entry.getKey(), e.getMessage());
                            }
                        }
                    });
            }

            // Remove files older than cleanup interval
            long cutoffTime = System.currentTimeMillis() - (cleanupIntervalHours * 60 * 60 * 1000L);
            cachedFiles.entrySet().stream()
                .filter(entry -> entry.getValue() < cutoffTime)
                .forEach(entry -> {
                    try {
                        Files.delete(entry.getKey());
                        log.info("Deleted expired cached file: {}", entry.getKey());
                    } catch (IOException e) {
                        log.error("Failed to delete expired cached file {}: {}", entry.getKey(), e.getMessage());
                    }
                });

            log.info("Thumbnail cache cleanup completed");

        } catch (IOException e) {
            log.error("Failed to cleanup thumbnail cache: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets cache statistics
     * @return cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            Path cachePath = Paths.get(cacheDir);
            if (!Files.exists(cachePath)) {
                stats.put("cacheDir", cacheDir);
                stats.put("totalFiles", 0);
                stats.put("totalSizeMB", 0);
                stats.put("maxSizeMB", maxCacheSizeMB);
                return stats;
            }

            long totalFiles = Files.list(cachePath)
                .filter(path -> path.getFileName().toString().startsWith("thumbnail_") && 
                              path.getFileName().toString().endsWith(".jpg"))
                .count();

            long totalSize = Files.list(cachePath)
                .filter(path -> path.getFileName().toString().startsWith("thumbnail_") && 
                              path.getFileName().toString().endsWith(".jpg"))
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();

            stats.put("cacheDir", cacheDir);
            stats.put("totalFiles", totalFiles);
            stats.put("totalSizeMB", totalSize / (1024 * 1024));
            stats.put("maxSizeMB", maxCacheSizeMB);
            stats.put("cleanupIntervalHours", cleanupIntervalHours);

        } catch (IOException e) {
            log.error("Failed to get cache stats: {}", e.getMessage());
            stats.put("error", e.getMessage());
        }

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
