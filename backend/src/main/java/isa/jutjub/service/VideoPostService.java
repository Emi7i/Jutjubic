package isa.jutjub.service;

import isa.jutjub.model.Videos;
import isa.jutjub.repository.VideoPostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class VideoPostService {

    private final VideoPostRepository videoPostRepository;
    private final FileUploadService fileUploadService;
    private final VideoPostCacheService videoPostCacheService;
    private final TileService tileService;

    @Autowired
    public VideoPostService(VideoPostRepository videoPostRepository, FileUploadService fileUploadService, VideoPostCacheService videoPostCacheService, TileService tileService) {
        this.videoPostRepository = videoPostRepository;
        this.fileUploadService = fileUploadService;
        this.videoPostCacheService = videoPostCacheService;
        this.tileService = tileService;
    }

    /**
     * Creates a new video post with transaction support
     * @param videos the video post to create
     * @param videoFile the video file to upload
     * @param thumbnailFile the thumbnail image file
     * @return the created video post
     * @throws RuntimeException if upload fails or transaction needs rollback
     */
    @Transactional(rollbackFor = Exception.class)
    public Videos createVideoPost(Videos videos, MultipartFile videoFile, MultipartFile thumbnailFile) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Starting video post creation for title: {}", videos.getTitle());
            
            // Validate video file
            if (videoFile == null || videoFile.isEmpty()) {
                throw new RuntimeException("Video file is required");
            }
            
            // Check video file size (200MB limit)
            if (videoFile.getSize() > 200 * 1024 * 1024) {
                throw new RuntimeException("Video file size exceeds 200MB limit");
            }
            
            // Check video file format
            if (!isVideoFileValid(videoFile)) {
                throw new RuntimeException("Invalid video file format. Only MP4 files are allowed");
            }
            
            // Upload video file with timeout monitoring
            String videoPath = fileUploadService.uploadVideoFile(videoFile);
            videos.setVideoPath(videoPath);
            videos.setVideoFileSize(videoFile.getSize());
            
            // Upload thumbnail if provided
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                String thumbnailPath = fileUploadService.uploadThumbnailFile(thumbnailFile);
                videos.setThumbnailPath(thumbnailPath);
            }
            
            // Set creation timestamp
            videos.setCreatedAt(LocalDateTime.now());
            
            // Calculate upload duration
            long endTime = System.currentTimeMillis();
            videos.setUploadDurationMs(endTime - startTime);

            videos.setVideoDuration(getVideoDuration(videos.getVideoPath()));
            
            // Save video post to database
            Videos savedPost = videoPostRepository.save(videos);
            
            log.info("Successfully created video post with ID: {}, upload duration: {}ms", 
                    savedPost.getId(), videos.getUploadDurationMs());
            
            // Automatically add video to tiles if it has valid coordinates
            Double longitude = savedPost.getLongitude();
            Double latitude = savedPost.getLatitude();
            log.info("Video {} location: '{}', parsed coordinates: ({}, {})", 
                    savedPost.getId(), savedPost.getLocation(), longitude, latitude);
            
            if (savedPost.hasValidCoordinates()) {
                try {
                    log.info("Auto-adding video {} to tiles based on coordinates: {}", 
                            savedPost.getId(), savedPost.getLocation());
                    tileService.addVideoToTile(savedPost);
                    log.info("Successfully added video {} to tiles", savedPost.getId());
                } catch (Exception e) {
                    log.warn("Failed to auto-add video {} to tiles: {}", savedPost.getId(), e.getMessage());
                    // Don't fail the video creation if tile assignment fails
                }
            } else {
                log.info("Video {} has no valid coordinates, skipping tile assignment", savedPost.getId());
            }
            
            return savedPost;
            
        } catch (Exception e) {
            log.error("Failed to create video post: {}", e.getMessage(), e);
            
            // Cleanup uploaded files if transaction fails
            if (videos.getVideoPath() != null) {
                fileUploadService.deleteFile(videos.getVideoPath());
            }
            if (videos.getThumbnailPath() != null) {
                fileUploadService.deleteFile(videos.getThumbnailPath());
            }
            
            throw new RuntimeException("Failed to create video post: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing video post
     * @param id the video post ID
     * @param videos the updated video post data
     * @return the updated video post
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "videoPosts", key = "#id")
    public Videos updateVideoPost(Long id, Videos videos) {
        log.debug("Updating video post {} and invalidating cache", id);
        
        Videos existingPost = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video post not found with ID: " + id));
        
        // Update allowed fields
        existingPost.setTitle(videos.getTitle());
        existingPost.setVideoDescription(videos.getVideoDescription());
        existingPost.setLocation(videos.getLocation());
        existingPost.setTags(videos.getTags());
        
        // Also invalidate the LoadingCache
        videoPostCacheService.invalidateVideoPost(id);
        
        return videoPostRepository.save(existingPost);
    }

    /**
     * Deletes a video post and associated files
     * @param id the video post ID
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "videoPosts", key = "#id")
    public void deleteVideoPost(Long id) {
        log.debug("Deleting video post {} and invalidating cache", id);
        
        Videos videos = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video post not found with ID: " + id));
        
        // Delete associated files
        if (videos.getVideoPath() != null) {
            fileUploadService.deleteFile(videos.getVideoPath());
        }
        if (videos.getThumbnailPath() != null) {
            fileUploadService.deleteFile(videos.getThumbnailPath());
        }
        
        // Delete from database
        videoPostRepository.delete(videos);
        
        // Also invalidate the LoadingCache
        videoPostCacheService.invalidateVideoPost(id);
        
        log.info("Successfully deleted video post with ID: {}", id);
    }

    /**
     * Gets a video post by ID
     * @param id the video post ID
     * @return the video post
     */
    @Transactional(readOnly = true)
    // @Cacheable(value = "videoPosts", key = "#id")
    public Videos getVideoPostById(Long id) {
        log.debug("Fetching video post by ID: {} without cache", id);
        
        return videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video post not found: " + id));
    }

    /**
     * Gets all video posts with pagination
     * @param pageable pagination information
     * @return page of video posts
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "videoPostsPage", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<Videos> getAllVideoPosts(Pageable pageable) {
        log.debug("Fetching all video posts with pagination: page {}, size {}", pageable.getPageNumber(), pageable.getPageSize());
        return videoPostRepository.findAll(pageable);
    }

    /**
     * Gets most recent video posts
     * @param pageable pagination information
     * @return page of most recent video posts
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "recentVideoPosts", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Videos> getMostRecentVideoPosts(Pageable pageable) {
        log.debug("Fetching most recent video posts: page {}, size {}", pageable.getPageNumber(), pageable.getPageSize());
        return videoPostRepository.findMostRecent(pageable);
    }

    /**
     * Gets most popular video posts
     * @param pageable pagination information
     * @return page of most popular video posts
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "popularVideoPosts", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Videos> getMostPopularVideoPosts(Pageable pageable) {
        log.debug("Fetching most popular video posts: page {}, size {}", pageable.getPageNumber(), pageable.getPageSize());
        return videoPostRepository.findMostPopular(pageable);
    }

    /**
     * Searches video posts by keyword
     * @param keyword search keyword
     * @param pageable pagination information
     * @return page of video posts matching search criteria
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "videoSearch", key = "#keyword + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Videos> searchVideoPosts(String keyword, Pageable pageable) {
        log.debug("Searching video posts with keyword '{}': page {}, size {}", keyword, pageable.getPageNumber(), pageable.getPageSize());
        return videoPostRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * Gets video posts by tag
     * @param tag the tag to search for
     * @param pageable pagination information
     * @return page of video posts with specified tag
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "videoPostsByTag", key = "#tag + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Videos> getVideoPostsByTag(String tag, Pageable pageable) {
        log.debug("Fetching video posts by tag '{}': page {}, size {}", tag, pageable.getPageNumber(), pageable.getPageSize());
        return videoPostRepository.findByTag(tag, pageable);
    }

    /**
     * Gets video posts by location
     * @param location the location to search for
     * @param pageable pagination information
     * @return page of video posts from specified location
     */
    @Transactional(readOnly = true)
    public Page<Videos> getVideoPostsByLocation(String location, Pageable pageable) {
        return videoPostRepository.findByLocationContainingIgnoreCase(location, pageable);
    }

    /**
     * Gets video posts created after specified date
     * @param fromDate date to search from
     * @param pageable pagination information
     * @return page of video posts created after specified date
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "videoPostsAfterDate", key = "#fromDate + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Videos> getVideoPostsAfterDate(LocalDateTime fromDate, Pageable pageable) {
        log.debug("Fetching video posts after date '{}': page {}, size {}", fromDate, pageable.getPageNumber(), pageable.getPageSize());
        return videoPostRepository.findByCreatedAtAfter(fromDate, pageable);
    }

    /**
     * Gets video posts created before specified date
     * @param toDate date to search until
     * @param pageable pagination information
     * @return page of video posts created before specified date
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "videoPostsBeforeDate", key = "#toDate + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Videos> getVideoPostsBeforeDate(LocalDateTime toDate, Pageable pageable) {
        log.debug("Fetching video posts before date '{}': page {}, size {}", toDate, pageable.getPageNumber(), pageable.getPageSize());
        return videoPostRepository.findByCreatedAtBefore(toDate, pageable);
    }

    /**
     * Gets video posts created within date range
     * @param fromDate start date
     * @param toDate end date
     * @param pageable pagination information
     * @return page of video posts created within date range
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "videoPostsDateRange", key = "#fromDate + '-' + #toDate + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Videos> getVideoPostsByDateRange(LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        log.debug("Fetching video posts between dates '{}' and '{}': page {}, size {}", fromDate, toDate, pageable.getPageNumber(), pageable.getPageSize());
        return videoPostRepository.findByCreatedAtBetween(fromDate, toDate, pageable);
    }

    /**
     * Increments the view count for a video post in a thread-safe manner
     * Uses atomic update to handle concurrent access correctly
     * @param id the video post ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long id) {
        // Use native SQL update for atomic operation - this handles concurrent access safely
        int updated = videoPostRepository.incrementViewsCount(id);
        
        if (updated == 0) {
            // If no rows were updated, the video post doesn't exist
            throw new RuntimeException("Video post not found with ID: " + id);
        }
        
        log.info("Incremented view count for video post ID: {}", id);
    }

    /**
     * Increments the like count for a video post
     * @param id the video post ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementLikeCount(Long id) {
        Videos videos = getVideoPostById(id);
        videos.setLikesCount(videos.getLikesCount() + 1);
        videoPostRepository.save(videos);
    }

    /**
     * Decrements the like count for a video post
     * @param id the video post ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void decrementLikeCount(Long id) {
        Videos videos = getVideoPostById(id);
        if (videos.getLikesCount() > 0) {
            videos.setLikesCount(videos.getLikesCount() - 1);
            videoPostRepository.save(videos);
        }
    }

    /**
     * Gets video file resource for streaming
     * @param filePath the file path
     * @return the video file resource
     */
    public Resource getVideoFile(String filePath) {
        return fileUploadService.getVideoFile(filePath);
    }

    /**
     * Gets thumbnail file resource
     * @param filePath the file path
     * @return the thumbnail file resource
     */
    public Resource getThumbnailFile(String filePath) {
        return fileUploadService.getThumbnailFile(filePath);
    }

    /**
     * Validates video file format
     * @param videoFile the video file to validate
     * @return true if valid, false otherwise
     */
    private boolean isVideoFileValid(MultipartFile videoFile) {
        String contentType = videoFile.getContentType();
        String originalFilename = videoFile.getOriginalFilename();
        
        // Check content type
        if (contentType == null || !contentType.equals("video/mp4")) {
            return false;
        }
        
        // Check file extension
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".mp4")) {
            return false;
        }
        
        return true;
    }

    /**
     * Gets popular tags from video posts
     * @return list of popular tags (top 10)
     */
    public List<String> getPopularTags() {
        log.info("Fetching popular tags");

        try {
            // Get all video posts
            List<Videos> allVideos = videoPostRepository.findAll();

            // Count tag frequencies
            Map<String, Integer> tagCounts = new java.util.HashMap<>();

            for (Videos video : allVideos) {
                List<String> tags = video.getTags();
                if (tags != null) {
                    for (String tag : tags) {
                        tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + 1);
                    }
                }
            }

            // Sort by frequency and get top 10
            List<String> popularTags = tagCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .map(Map.Entry::getKey)
                    .collect(java.util.stream.Collectors.toList());

            log.info("Found {} popular tags: {}", popularTags.size(), popularTags);
            return popularTags;

        } catch (Exception e) {
            log.error("Error fetching popular tags: {}", e.getMessage(), e);
            // Return fallback tags
            return java.util.Arrays.asList("music", "gaming", "education", "sports", "comedy", "entertainment", "news", "technology", "tutorial", "vlog");
        }
    }


    private double getVideoDuration(String url) throws IOException, InterruptedException {

        Path absPath = Paths.get(url).toAbsolutePath().normalize();

        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                absPath.toString()
        );
        log.info("ffprobe call: " + pb.toString());
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        String output = reader.readLine();
        process.waitFor();

        return output != null ? Double.parseDouble(output) : 0.0;
    }
}
