package isa.jutjub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import isa.jutjub.model.Videos;
import isa.jutjub.service.VideoEventPublisher;
import isa.jutjub.service.VideoPostService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/video-posts")
@Tag(name = "Video Posts", description = "Video post management endpoints")
@Slf4j
public class VideoPostController {

    private final VideoPostService videoPostService;

    private final VideoEventPublisher videoEventPublisher;

    @Autowired
    public VideoPostController(VideoPostService videoPostService, VideoEventPublisher videoEventPublisher) {
        this.videoPostService = videoPostService;
        this.videoEventPublisher = videoEventPublisher;
    }

    /**
     * Creates a new video post with video and thumbnail files
     */
    @Operation(summary = "Create a new video post", description = "Upload a video with thumbnail and metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Video post created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or file format"),
            @ApiResponse(responseCode = "413", description = "File too large"),
            @ApiResponse(responseCode = "408", description = "Upload timeout"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createVideoPost(
            @Parameter(description = "Video post metadata", required = true)
            @RequestPart("videos") @Valid Videos videos,

            @Parameter(description = "Video file (MP4, max 200MB)", required = true)
            @RequestPart("videoFile") MultipartFile videoFile,

            @Parameter(description = "Thumbnail image file (optional)")
            @RequestPart(value = "thumbnailFile", required = false) MultipartFile thumbnailFile) {

        try {
            Videos createdPost = videoPostService.createVideoPost(videos, videoFile, thumbnailFile);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Video post created successfully");
            response.put("data", createdPost);


            try {
                System.out.println("About to publish event for video ID: " + createdPost.getId());
                videoEventPublisher.publishVideoUploadEvent(createdPost);
                System.out.println("Video event published for video ID: " + createdPost.getId());
            } catch (Exception e) {
                // Log but don't fail the upload if messaging fails
                System.out.println("Failed to publish video event to RabbitMQ: " + e.getMessage());
                // You could also add this to response as a warning
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to create video post: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to create video post: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Gets all video posts with pagination and optional date filtering
     */
    @Operation(summary = "Get all video posts", description = "Retrieve paginated list of video posts with optional date filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Video posts retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllVideoPosts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDir,

            @Parameter(description = "From date (ISO format: YYYY-MM-DDTHH:mm:ss.sssZ)")
            @RequestParam(required = false) String from,

            @Parameter(description = "To date (ISO format: YYYY-MM-DDTHH:mm:ss.sssZ)")
            @RequestParam(required = false) String to) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<Videos> videoPosts;

            // Handle date filtering if parameters are provided
            if (from != null || to != null) {
                LocalDateTime fromDate = null;
                LocalDateTime toDate = null;

                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

                if (from != null && !from.trim().isEmpty()) {
                    try {
                        fromDate = LocalDateTime.parse(from, formatter);
                    } catch (DateTimeParseException e) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("success", false);
                        error.put("message", "Invalid 'from' date format. Use: YYYY-MM-DDTHH:mm:ss.sssZ");
                        return ResponseEntity.badRequest().body(error);
                    }
                }

                if (to != null && !to.trim().isEmpty()) {
                    try {
                        toDate = LocalDateTime.parse(to, formatter);
                    } catch (DateTimeParseException e) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("success", false);
                        error.put("message", "Invalid 'to' date format. Use: YYYY-MM-DDTHH:mm:ss.sssZ");
                        return ResponseEntity.badRequest().body(error);
                    }
                }

                if (fromDate != null && toDate != null) {
                    videoPosts = videoPostService.getVideoPostsByDateRange(fromDate, toDate, pageable);
                } else if (fromDate != null) {
                    videoPosts = videoPostService.getVideoPostsAfterDate(fromDate, pageable);
                } else {
                    videoPosts = videoPostService.getVideoPostsBeforeDate(toDate, pageable);
                }
            } else {
                videoPosts = videoPostService.getAllVideoPosts(pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", videoPosts.getContent());
            response.put("currentPage", videoPosts.getNumber());
            response.put("totalItems", videoPosts.getTotalElements());
            response.put("totalPages", videoPosts.getTotalPages());
            response.put("pageSize", videoPosts.getSize());

            // Add filter info if date filtering is applied
            if (from != null || to != null) {
                Map<String, Object> filterInfo = new HashMap<>();
                if (from != null) {
                    filterInfo.put("from", from);
                }
                if (to != null) {
                    filterInfo.put("to", to);
                }
                response.put("filter", filterInfo);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to retrieve video posts: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve video posts: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Gets a specific video post by ID
     */
    @Operation(summary = "Get video post by ID", description = "Retrieve a specific video post")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Video post retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Video post not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getVideoPostById(
            @Parameter(description = "Video post ID", required = true)
            @PathVariable Long id) {

        try {
            Videos videos = videoPostService.getVideoPostById(id);

            // Increment view count
            videoPostService.incrementViewCount(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", videos);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Failed to retrieve video post with ID {}: {}", id, e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Gets the most recent video posts
     */
    @Operation(summary = "Get most recent video posts", description = "Retrieve recently uploaded video posts")
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentVideoPosts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Videos> videoPosts = videoPostService.getMostRecentVideoPosts(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", videoPosts.getContent());
            response.put("currentPage", videoPosts.getNumber());
            response.put("totalItems", videoPosts.getTotalElements());
            response.put("totalPages", videoPosts.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to retrieve recent video posts: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve recent video posts: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Searches video posts by keyword
     */
    @Operation(summary = "Search video posts", description = "Search video posts by keyword in title, description, tags, or location")
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchVideoPosts(
            @Parameter(description = "Search keyword", required = true)
            @RequestParam String keyword,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Videos> videoPosts = videoPostService.searchVideoPosts(keyword, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", videoPosts.getContent());
            response.put("currentPage", videoPosts.getNumber());
            response.put("totalItems", videoPosts.getTotalElements());
            response.put("totalPages", videoPosts.getTotalPages());
            response.put("searchKeyword", keyword);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to search video posts with keyword '{}': {}", keyword, e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to search video posts: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Gets video posts by tag
     */
    @Operation(summary = "Get video posts by tag", description = "Retrieve video posts with specific tag")
    @GetMapping("/tag/{tag}")
    public ResponseEntity<Map<String, Object>> getVideoPostsByTag(
            @Parameter(description = "Tag to search for", required = true)
            @PathVariable String tag,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Videos> videoPosts = videoPostService.getVideoPostsByTag(tag, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", videoPosts.getContent());
            response.put("currentPage", videoPosts.getNumber());
            response.put("totalItems", videoPosts.getTotalElements());
            response.put("totalPages", videoPosts.getTotalPages());
            response.put("tag", tag);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to retrieve video posts by tag '{}': {}", tag, e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve video posts by tag: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Likes a video post
     */
    @Operation(summary = "Like a video post", description = "Increment like count for a video post")
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> likeVideoPost(
            @Parameter(description = "Video post ID", required = true)
            @PathVariable Long id) {

        try {
            videoPostService.incrementLikeCount(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Video post liked successfully");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Failed to like video post with ID {}: {}", id, e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Unlikes a video post
     */
    @Operation(summary = "Unlike a video post", description = "Decrement like count for a video post")
    @PostMapping("/{id}/unlike")
    public ResponseEntity<Map<String, Object>> unlikeVideoPost(
            @Parameter(description = "Video post ID", required = true)
            @PathVariable Long id) {

        try {
            videoPostService.decrementLikeCount(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Video post unliked successfully");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Failed to unlike video post with ID {}: {}", id, e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Gets view count statistics for a video post
     */
    @Operation(summary = "Get view count statistics", description = "Get current view count and statistics for a video")
    @GetMapping("/{id}/views")
    public ResponseEntity<Map<String, Object>> getViewCount(@PathVariable Long id) {
        try {
            Videos videos = videoPostService.getVideoPostById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("videoId", id);
            response.put("title", videos.getTitle());
            response.put("viewsCount", videos.getViewsCount());
            response.put("lastAccessed", videos.getUpdatedAt());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Failed to get view count for video post ID {}: {}", id, e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Test endpoint to simulate concurrent view increments
     */
    @Operation(summary = "Test concurrent views", description = "Simulate concurrent access to test view counting")
    @PostMapping("/{id}/simulate-views")
    public ResponseEntity<Map<String, Object>> simulateConcurrentViews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int threads,
            @RequestParam(defaultValue = "5") int viewsPerThread) {

        try {
            Videos videos = videoPostService.getVideoPostById(id);
            long initialViews = videos.getViewsCount();

            ExecutorService executorService = Executors.newFixedThreadPool(threads);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            AtomicInteger successfulIncrements = new AtomicInteger(0);
            AtomicInteger failedIncrements = new AtomicInteger(0);

            long startTime = System.currentTimeMillis();

            // Simulate concurrent view increments
            for (int i = 0; i < threads; i++) {
                final int threadId = i;

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < viewsPerThread; j++) {
                        try {
                            videoPostService.incrementViewCount(id);
                            successfulIncrements.incrementAndGet();
                            Thread.sleep(1); // Small delay
                        } catch (Exception e) {
                            failedIncrements.incrementAndGet();
                            log.warn("Thread {} increment {} failed: {}", threadId, j, e.getMessage());
                        }
                    }
                }, executorService);

                futures.add(future);
            }

            // Wait for completion
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);

            executorService.shutdown();

            long endTime = System.currentTimeMillis();

            // Get final view count
            Videos updatedVideo = videoPostService.getVideoPostById(id);
            long finalViews = updatedVideo.getViewsCount();
            long expectedIncrement = (long) threads * viewsPerThread;
            long actualIncrement = finalViews - initialViews;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("videoId", id);
            response.put("title", videos.getTitle());
            response.put("testParameters", Map.of(
                    "threads", threads,
                    "viewsPerThread", viewsPerThread,
                    "totalExpectedIncrements", expectedIncrement
            ));
            response.put("results", Map.of(
                    "initialViews", initialViews,
                    "finalViews", finalViews,
                    "actualIncrement", actualIncrement,
                    "successfulIncrements", successfulIncrements.get(),
                    "failedIncrements", failedIncrements.get(),
                    "durationMs", endTime - startTime,
                    "testPassed", actualIncrement == expectedIncrement && failedIncrements.get() == 0
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to simulate concurrent views for video post ID {}: {}", id, e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to simulate concurrent views: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Test endpoint to verify video file access
     */
    @GetMapping("/{id}/video-test")
    public ResponseEntity<Map<String, Object>> testVideoAccess(@PathVariable Long id) {
        try {
            Videos videos = videoPostService.getVideoPostById(id);
            Resource videoResource = videoPostService.getVideoFile(videos.getVideoPath());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("videoId", id);
            response.put("videoPath", videos.getVideoPath());
            response.put("resourceExists", videoResource.exists());
            response.put("resourceReadable", videoResource.isReadable());
            response.put("resourceFilename", videoResource.getFilename());

            try {
                response.put("fileSize", videoResource.contentLength());
            } catch (IOException e) {
                response.put("fileSize", "Error: " + e.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Serves video files
     */
    @Operation(summary = "Serve video file", description = "Stream video file for playback")
    @GetMapping("/{id}/video")
    public ResponseEntity<Resource> serveVideo(
            @Parameter(description = "Video post ID", required = true)
            @PathVariable Long id) {

        try {
            Videos videos = videoPostService.getVideoPostById(id);
            Resource videoResource = videoPostService.getVideoFile(videos.getVideoPath());

            String contentType = "video/mp4";
            String filename = videoResource.getFilename();
            if (filename != null) {
                if (filename.endsWith(".webm")) {
                    contentType = "video/webm";
                } else if (filename.endsWith(".ogg") || filename.endsWith(".ogv")) {
                    contentType = "video/ogg";
                } else if (filename.endsWith(".mov") || filename.endsWith(".qt")) {
                    contentType = "video/quicktime";
                } else if (filename.endsWith(".avi")) {
                    contentType = "video/x-msvideo";
                }
            }

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + videos.getTitle() + ".mp4\"")
                    .body(videoResource);

        } catch (RuntimeException e) {
            log.error("Failed to serve video for post ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Gets popular tags
     */
    @Operation(summary = "Get popular tags", description = "Retrieve most popular tags from video posts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Popular tags retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/tags/popular")
    public ResponseEntity<Map<String, Object>> getPopularTags() {
        try {
            List<String> popularTags = videoPostService.getPopularTags();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", popularTags);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to retrieve popular tags: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve popular tags: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Serves thumbnail images
     */
    @Operation(summary = "Serve thumbnail image", description = "Serve thumbnail image for video post")
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> serveThumbnail(
            @Parameter(description = "Video post ID", required = true)
            @PathVariable Long id) {

        try {
            Videos videos = videoPostService.getVideoPostById(id);

            if (videos.getThumbnailPath() == null) {
                return ResponseEntity.notFound().build();
            }

            Resource thumbnailResource = videoPostService.getThumbnailFile(videos.getThumbnailPath());

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"thumbnail_" + id + ".jpg\"")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET")
                    .header("Access-Control-Allow-Headers", "*")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600") // Cache for 1 hour
                    .body(thumbnailResource);

        } catch (RuntimeException e) {
            log.error("Failed to serve thumbnail for post ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Gets the top 3 popular videos from the latest ETL run
     */
    @Operation(summary = "Get top popular videos", description = "Retrieve top 3 popular videos from the latest ETL pipeline run")
    @GetMapping("/top-popular")
    public ResponseEntity<Map<String, Object>> getTopPopularVideos() {
        try {
            List<Map<String, Object>> popularVideos = videoPostService.getTopPopularVideos();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", popularVideos);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to retrieve top popular videos: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve top popular videos: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}