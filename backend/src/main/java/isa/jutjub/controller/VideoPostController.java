package isa.jutjub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import isa.jutjub.model.VideoPost;
import isa.jutjub.service.VideoPostService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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

@RestController
@RequestMapping("/api/video-posts")
@Tag(name = "Video Posts", description = "Video post management endpoints")
@Slf4j
public class VideoPostController {

    private final VideoPostService videoPostService;

    @Autowired
    public VideoPostController(VideoPostService videoPostService) {
        this.videoPostService = videoPostService;
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
            @RequestPart("videoPost") @Valid VideoPost videoPost,
            
            @Parameter(description = "Video file (MP4, max 200MB)", required = true)
            @RequestPart("videoFile") MultipartFile videoFile,
            
            @Parameter(description = "Thumbnail image file (optional)")
            @RequestPart(value = "thumbnailFile", required = false) MultipartFile thumbnailFile) {
        
        try {
            VideoPost createdPost = videoPostService.createVideoPost(videoPost, videoFile, thumbnailFile);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Video post created successfully");
            response.put("data", createdPost);
            
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
     * Gets all video posts with pagination
     */
    @Operation(summary = "Get all video posts", description = "Retrieve paginated list of video posts")
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
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<VideoPost> videoPosts = videoPostService.getAllVideoPosts(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", videoPosts.getContent());
            response.put("currentPage", videoPosts.getNumber());
            response.put("totalItems", videoPosts.getTotalElements());
            response.put("totalPages", videoPosts.getTotalPages());
            response.put("pageSize", videoPosts.getSize());
            
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
            VideoPost videoPost = videoPostService.getVideoPostById(id);
            
            // Increment view count
            videoPostService.incrementViewCount(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", videoPost);
            
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
            Page<VideoPost> videoPosts = videoPostService.getMostRecentVideoPosts(pageable);
            
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
     * Gets the most popular video posts
     */
    @Operation(summary = "Get most popular video posts", description = "Retrieve video posts sorted by likes")
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularVideoPosts(
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<VideoPost> videoPosts = videoPostService.getMostPopularVideoPosts(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", videoPosts.getContent());
            response.put("currentPage", videoPosts.getNumber());
            response.put("totalItems", videoPosts.getTotalElements());
            response.put("totalPages", videoPosts.getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve popular video posts: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve popular video posts: " + e.getMessage());
            
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
            Page<VideoPost> videoPosts = videoPostService.searchVideoPosts(keyword, pageable);
            
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
            Page<VideoPost> videoPosts = videoPostService.getVideoPostsByTag(tag, pageable);
            
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
     * Gets view count statistics for a video post
     */
    @Operation(summary = "Get view count statistics", description = "Get current view count and statistics for a video")
    @GetMapping("/{id}/views")
    public ResponseEntity<Map<String, Object>> getViewCount(@PathVariable Long id) {
        try {
            VideoPost videoPost = videoPostService.getVideoPostById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("videoId", id);
            response.put("title", videoPost.getTitle());
            response.put("viewsCount", videoPost.getViewsCount());
            response.put("lastAccessed", videoPost.getUpdatedAt());
            
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
            VideoPost videoPost = videoPostService.getVideoPostById(id);
            long initialViews = videoPost.getViewsCount();
            
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
            VideoPost updatedVideo = videoPostService.getVideoPostById(id);
            long finalViews = updatedVideo.getViewsCount();
            long expectedIncrement = (long) threads * viewsPerThread;
            long actualIncrement = finalViews - initialViews;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("videoId", id);
            response.put("title", videoPost.getTitle());
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
            VideoPost videoPost = videoPostService.getVideoPostById(id);
            Resource videoResource = videoPostService.getVideoFile(videoPost.getVideoPath());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("videoId", id);
            response.put("videoPath", videoPost.getVideoPath());
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
            VideoPost videoPost = videoPostService.getVideoPostById(id);
            Resource videoResource = videoPostService.getVideoFile(videoPost.getVideoPath());
            
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
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + videoPost.getTitle() + ".mp4\"")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                    .header("Access-Control-Allow-Headers", "Range, Content-Type")
                    .body(videoResource);
            
        } catch (RuntimeException e) {
            log.error("Failed to serve video for post ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
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
            VideoPost videoPost = videoPostService.getVideoPostById(id);
            
            if (videoPost.getThumbnailPath() == null) {
                return ResponseEntity.notFound().build();
            }
            
            Resource thumbnailResource = videoPostService.getThumbnailFile(videoPost.getThumbnailPath());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"thumbnail_" + id + ".jpg\"")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET")
                    .header("Access-Control-Allow-Headers", "*")
                    .body(thumbnailResource);
            
        } catch (RuntimeException e) {
            log.error("Failed to serve thumbnail for post ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Gets comments for a video post
     */
    @Operation(summary = "Get video comments", description = "Retrieve all comments for a video post")
    @GetMapping("/{id}/comments")
    public ResponseEntity<Map<String, Object>> getVideoComments(
            @Parameter(description = "Video post ID", required = true)
            @PathVariable Long id) {
        
        try {
            // TODO: Implement actual comments retrieval from database
            // For now, return empty list
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", new Object[0]); // Empty array for now
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve comments for video post ID {}: {}", id, e.getMessage());
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve comments: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Adds a comment to a video post
     */
    @Operation(summary = "Add comment", description = "Add a new comment to a video post")
    @PostMapping("/{id}/comments")
    public ResponseEntity<Map<String, Object>> addVideoComment(
            @Parameter(description = "Video post ID", required = true)
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Comment text is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            // TODO: Implement actual comment creation in database
            // For now, return a mock comment
            Map<String, Object> comment = new HashMap<>();
            comment.put("id", System.currentTimeMillis());
            comment.put("videoId", id);
            comment.put("userId", "1");
            comment.put("userName", "Anonymous User");
            comment.put("text", text.trim());
            comment.put("createdAt", java.time.LocalDateTime.now());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", comment);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Failed to add comment to video post ID {}: {}", id, e.getMessage());
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to add comment: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
