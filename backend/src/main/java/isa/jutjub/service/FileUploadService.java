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
public class FileUploadService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.upload.video.max-size-mb:200}")
    private int maxVideoSizeMB;

    @Value("${app.upload.timeout-minutes:30}")
    private int uploadTimeoutMinutes;

    private final ThreadPoolExecutor uploadExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

    /**
     * Uploads video file with timeout monitoring
     * @param videoFile the video file to upload
     * @return the file path where video was saved
     * @throws RuntimeException if upload fails or times out
     */
    public String uploadVideoFile(MultipartFile videoFile) {
        log.info("Starting video file upload: {}", videoFile.getOriginalFilename());
        
        try {
            // Validate file size
            long maxSizeBytes = maxVideoSizeMB * 1024L * 1024L;
            if (videoFile.getSize() > maxSizeBytes) {
                throw new RuntimeException("Video file size exceeds " + maxVideoSizeMB + "MB limit");
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, "videos");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = videoFile.getOriginalFilename();
            String fileExtension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".mp4";
            String uniqueFilename = generateUniqueFilename("video", fileExtension);
            Path filePath = uploadPath.resolve(uniqueFilename);

            // Upload with timeout monitoring
            CompletableFuture<String> uploadFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    Files.copy(videoFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Successfully uploaded video file to: {}", filePath);
                    return filePath.toString();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save video file: " + e.getMessage(), e);
                }
            }, uploadExecutor);

            // Wait for upload to complete with timeout
            try {
                String savedPath = uploadFuture.get(uploadTimeoutMinutes, TimeUnit.MINUTES);
                
                // Verify file was saved correctly
                if (!Files.exists(filePath) || Files.size(filePath) == 0) {
                    throw new RuntimeException("Video file upload verification failed");
                }
                
                return savedPath;
                
            } catch (TimeoutException e) {
                uploadFuture.cancel(true);
                cleanupFailedUpload(filePath);
                throw new RuntimeException("Video upload timed out after " + uploadTimeoutMinutes + " minutes");
            } catch (Exception e) {
                cleanupFailedUpload(filePath);
                throw new RuntimeException("Video upload failed: " + e.getMessage(), e);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads thumbnail image file
     * @param thumbnailFile the thumbnail file to upload
     * @return the file path where thumbnail was saved
     * @throws RuntimeException if upload fails
     */
    public String uploadThumbnailFile(MultipartFile thumbnailFile) {
        log.info("Starting thumbnail file upload: {}", thumbnailFile.getOriginalFilename());
        
        try {
            // Validate thumbnail file
            if (!isImageFileValid(thumbnailFile)) {
                throw new RuntimeException("Invalid thumbnail file format. Only JPG, PNG, and GIF files are allowed");
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, "thumbnails");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = thumbnailFile.getOriginalFilename();
            String fileExtension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String uniqueFilename = generateUniqueFilename("thumbnail", fileExtension);
            Path filePath = uploadPath.resolve(uniqueFilename);

            // Save thumbnail file
            Files.copy(thumbnailFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Verify file was saved correctly
            if (!Files.exists(filePath) || Files.size(filePath) == 0) {
                throw new RuntimeException("Thumbnail file upload verification failed");
            }
            
            log.info("Successfully uploaded thumbnail file to: {}", filePath);
            return filePath.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload thumbnail file: " + e.getMessage(), e);
        }
    }

    /**
     * Gets video file resource for streaming
     * @param filePath the file path
     * @return the video file resource
     * @throws RuntimeException if file not found
     */
    public Resource getVideoFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Video file not found: " + filePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load video file: " + e.getMessage(), e);
        }
    }

    /**
     * Gets thumbnail file resource
     * @param filePath the file path
     * @return the thumbnail file resource
     * @throws RuntimeException if file not found
     */
    public Resource getThumbnailFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Thumbnail file not found: " + filePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load thumbnail file: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a file from the filesystem
     * @param filePath the file path to delete
     */
    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Successfully deleted file: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file {}: {}", filePath, e.getMessage());
        }
    }

    /**
     * Validates image file format
     * @param imageFile the image file to validate
     * @return true if valid, false otherwise
     */
    private boolean isImageFileValid(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        String originalFilename = imageFile.getOriginalFilename();
        
        // Check content type
        if (contentType == null || (!contentType.equals("image/jpeg") && 
            !contentType.equals("image/jpg") && 
            !contentType.equals("image/png") && 
            !contentType.equals("image/gif"))) {
            return false;
        }
        
        // Check file extension
        if (originalFilename == null) {
            return false;
        }
        
        String lowerCaseFilename = originalFilename.toLowerCase();
        return lowerCaseFilename.endsWith(".jpg") || 
               lowerCaseFilename.endsWith(".jpeg") || 
               lowerCaseFilename.endsWith(".png") || 
               lowerCaseFilename.endsWith(".gif");
    }

    /**
     * Generates a unique filename with timestamp and UUID
     * @param prefix the filename prefix
     * @param extension the file extension
     * @return unique filename
     */
    private String generateUniqueFilename(String prefix, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return prefix + "_" + timestamp + "_" + uuid + extension;
    }

    /**
     * Cleans up failed upload files
     * @param filePath the file path to clean up
     */
    private void cleanupFailedUpload(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Cleaned up failed upload file: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to cleanup upload file {}: {}", filePath, e.getMessage());
        }
    }

    /**
     * Gets upload statistics
     * @return upload statistics
     */
    public Map<String, Object> getUploadStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("maxVideoSizeMB", maxVideoSizeMB);
        stats.put("uploadTimeoutMinutes", uploadTimeoutMinutes);
        stats.put("uploadDir", uploadDir);
        stats.put("activeThreads", uploadExecutor.getActiveCount());
        return stats;
    }

    /**
     * Shuts down the upload executor service
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down FileUploadService...");
        uploadExecutor.shutdown();
        try {
            if (!uploadExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                uploadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            uploadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
