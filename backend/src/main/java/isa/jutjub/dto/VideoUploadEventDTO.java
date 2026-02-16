package isa.jutjub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class VideoUploadEventDTO {

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
    public static class Location {
        private Double latitude;
        private Double longitude;
        private String address;
    }
}