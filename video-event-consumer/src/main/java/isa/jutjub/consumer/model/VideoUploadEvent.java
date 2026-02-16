package isa.jutjub.consumer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadEvent {
    
    @JsonProperty("video_id")
    private Long videoId;
    
    private String title;
    
    private String description;
    
    @JsonProperty("uploader_id")
    private String uploaderId;
    
    @JsonProperty("file_size_bytes")
    private Long fileSizeBytes;
    
    @JsonProperty("duration_seconds")
    private Double durationSeconds;
    
    private String format;
    
    @JsonProperty("upload_timestamp")
    private Long uploadTimestamp;
    
    private List<String> tags;
    
    private Location location;
    
    // URLs instead of raw data
    @JsonProperty("video_url")
    private String videoUrl;
    
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
    
    // File paths
    @JsonProperty("video_path")
    private String videoPath;
    
    @JsonProperty("thumbnail_path")
    private String thumbnailPath;
    
    // Counters
    @JsonProperty("views_count")
    private Long viewsCount;
    
    @JsonProperty("likes_count")
    private Long likesCount;
    
    @JsonProperty("comments_count")
    private Long commentsCount;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private Double latitude;
        private Double longitude;
        private String address;
    }
    
    /**
     * Get file size in MB
     */
    public double getFileSizeMB() {
        return fileSizeBytes != null ? fileSizeBytes / (1024.0 * 1024.0) : 0;
    }
    
    /**
     * Get duration in minutes
     */
    public double getDurationMinutes() {
        return durationSeconds != null ? durationSeconds / 60.0 : 0;
    }
}
