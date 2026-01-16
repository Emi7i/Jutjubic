package isa.jutjub.service;

import isa.jutjub.model.VideoPost;
import isa.jutjub.repository.VideoPostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@Transactional
public class VideoPostService {

    private final VideoPostRepository videoPostRepository;
    private final FileUploadService fileUploadService;

    @Autowired
    public VideoPostService(VideoPostRepository videoPostRepository, FileUploadService fileUploadService) {
        this.videoPostRepository = videoPostRepository;
        this.fileUploadService = fileUploadService;
    }

    /**
     * Creates a new video post with transaction support
     * @param videoPost the video post to create
     * @param videoFile the video file to upload
     * @param thumbnailFile the thumbnail image file
     * @return the created video post
     * @throws RuntimeException if upload fails or transaction needs rollback
     */
    @Transactional(rollbackFor = Exception.class)
    public VideoPost createVideoPost(VideoPost videoPost, MultipartFile videoFile, MultipartFile thumbnailFile) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Starting video post creation for title: {}", videoPost.getTitle());
            
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
            videoPost.setVideoPath(videoPath);
            videoPost.setVideoFileSize(videoFile.getSize());
            
            // Upload thumbnail if provided
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                String thumbnailPath = fileUploadService.uploadThumbnailFile(thumbnailFile);
                videoPost.setThumbnailPath(thumbnailPath);
            }
            
            // Set creation timestamp
            videoPost.setCreatedAt(LocalDateTime.now());
            
            // Calculate upload duration
            long endTime = System.currentTimeMillis();
            videoPost.setUploadDurationMs(endTime - startTime);
            
            // Save video post to database
            VideoPost savedPost = videoPostRepository.save(videoPost);
            
            log.info("Successfully created video post with ID: {}, upload duration: {}ms", 
                    savedPost.getId(), videoPost.getUploadDurationMs());
            
            return savedPost;
            
        } catch (Exception e) {
            log.error("Failed to create video post: {}", e.getMessage(), e);
            
            // Cleanup uploaded files if transaction fails
            if (videoPost.getVideoPath() != null) {
                fileUploadService.deleteFile(videoPost.getVideoPath());
            }
            if (videoPost.getThumbnailPath() != null) {
                fileUploadService.deleteFile(videoPost.getThumbnailPath());
            }
            
            throw new RuntimeException("Failed to create video post: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing video post
     * @param id the video post ID
     * @param videoPost the updated video post data
     * @return the updated video post
     */
    @Transactional(rollbackFor = Exception.class)
    public VideoPost updateVideoPost(Long id, VideoPost videoPost) {
        VideoPost existingPost = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video post not found with ID: " + id));
        
        // Update allowed fields
        existingPost.setTitle(videoPost.getTitle());
        existingPost.setVideoDescription(videoPost.getVideoDescription());
        existingPost.setLocation(videoPost.getLocation());
        existingPost.setTags(videoPost.getTags());
        
        return videoPostRepository.save(existingPost);
    }

    /**
     * Deletes a video post and associated files
     * @param id the video post ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteVideoPost(Long id) {
        VideoPost videoPost = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video post not found with ID: " + id));
        
        // Delete associated files
        if (videoPost.getVideoPath() != null) {
            fileUploadService.deleteFile(videoPost.getVideoPath());
        }
        if (videoPost.getThumbnailPath() != null) {
            fileUploadService.deleteFile(videoPost.getThumbnailPath());
        }
        
        // Delete from database
        videoPostRepository.delete(videoPost);
        
        log.info("Successfully deleted video post with ID: {}", id);
    }

    /**
     * Gets a video post by ID
     * @param id the video post ID
     * @return the video post
     */
    @Transactional(readOnly = true)
    public VideoPost getVideoPostById(Long id) {
        return videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video post not found with ID: " + id));
    }

    /**
     * Gets all video posts with pagination
     * @param pageable pagination information
     * @return page of video posts
     */
    @Transactional(readOnly = true)
    public Page<VideoPost> getAllVideoPosts(Pageable pageable) {
        return videoPostRepository.findAll(pageable);
    }

    /**
     * Gets most recent video posts
     * @param pageable pagination information
     * @return page of most recent video posts
     */
    @Transactional(readOnly = true)
    public Page<VideoPost> getMostRecentVideoPosts(Pageable pageable) {
        return videoPostRepository.findMostRecent(pageable);
    }

    /**
     * Gets most popular video posts
     * @param pageable pagination information
     * @return page of most popular video posts
     */
    @Transactional(readOnly = true)
    public Page<VideoPost> getMostPopularVideoPosts(Pageable pageable) {
        return videoPostRepository.findMostPopular(pageable);
    }

    /**
     * Searches video posts by keyword
     * @param keyword search keyword
     * @param pageable pagination information
     * @return page of video posts matching search criteria
     */
    @Transactional(readOnly = true)
    public Page<VideoPost> searchVideoPosts(String keyword, Pageable pageable) {
        return videoPostRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * Gets video posts by tag
     * @param tag the tag to search for
     * @param pageable pagination information
     * @return page of video posts with specified tag
     */
    @Transactional(readOnly = true)
    public Page<VideoPost> getVideoPostsByTag(String tag, Pageable pageable) {
        return videoPostRepository.findByTag(tag, pageable);
    }

    /**
     * Gets video posts by location
     * @param location the location to search for
     * @param pageable pagination information
     * @return page of video posts from specified location
     */
    @Transactional(readOnly = true)
    public Page<VideoPost> getVideoPostsByLocation(String location, Pageable pageable) {
        return videoPostRepository.findByLocationContainingIgnoreCase(location, pageable);
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
        VideoPost videoPost = getVideoPostById(id);
        videoPost.setLikesCount(videoPost.getLikesCount() + 1);
        videoPostRepository.save(videoPost);
    }

    /**
     * Decrements the like count for a video post
     * @param id the video post ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void decrementLikeCount(Long id) {
        VideoPost videoPost = getVideoPostById(id);
        if (videoPost.getLikesCount() > 0) {
            videoPost.setLikesCount(videoPost.getLikesCount() - 1);
            videoPostRepository.save(videoPost);
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
}
