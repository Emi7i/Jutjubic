package isa.jutjub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "video_posts")
@Getter
@Setter
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "isa.jutjub.model.VideoPost")
public class VideoPost extends BaseEntity {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Video description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String videoDescription;

    @ElementCollection
    @CollectionTable(name = "video_post_tags", joinColumns = @JoinColumn(name = "video_post_id"))
    @Column(name = "tag")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<String> tags = new HashSet<>();

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    @Column(name = "video_path", nullable = false)
    private String videoPath;

    @Column(name = "video_file_size")
    private Long videoFileSize;

    @Column(name = "upload_duration_ms")
    private Long uploadDurationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "location")
    private String location;

    // Interaction counters
    @Column(name = "likes_count")
    private Long likesCount = 0L;

    @Column(name = "comments_count")
    private Long commentsCount = 0L;

    @Column(name = "views_count")
    private Long viewsCount = 0L;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Adds a tag to the video post
     * @param tag the tag to add
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            tags.add(tag.trim().toLowerCase());
        }
    }

    /**
     * Removes a tag from the video post
     * @param tag the tag to remove
     */
    public void removeTag(String tag) {
        if (tag != null) {
            tags.remove(tag.trim().toLowerCase());
        }
    }

    /**
     * Gets all tags as a comma-separated string
     * @return comma-separated tags
     */
    public String getTagsAsString() {
        return String.join(", ", tags);
    }

    /**
     * Sets tags from a comma-separated string
     * @param tagsString comma-separated tags
     */
    public void setTagsFromString(String tagsString) {
        tags.clear();
        if (tagsString != null && !tagsString.trim().isEmpty()) {
            String[] tagArray = tagsString.split(",");
            for (String tag : tagArray) {
                addTag(tag);
            }
        }
    }

    /**
     * Parse longitude from location string
     * Expected format: "longitude,latitude" or {"longitude":X,"latitude":Y}
     * @return longitude as Integer, or null if not found
     */
    public Integer getLongitude() {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try JSON format first
            if (location.contains("\"longitude\"") && location.contains("\"latitude\"")) {
                // Extract longitude using regex-like approach
                String pattern = "\"longitude\":";
                int index = location.indexOf(pattern);
                if (index != -1) {
                    int start = index + pattern.length();
                    // Skip whitespace
                    while (start < location.length() && location.charAt(start) == ' ') {
                        start++;
                    }
                    // Find the end of the number
                    int end = start;
                    while (end < location.length() && 
                           (Character.isDigit(location.charAt(end)) || location.charAt(end) == '-' || location.charAt(end) == '.')) {
                        end++;
                    }
                    if (end > start) {
                        String lonStr = location.substring(start, end).trim();
                        return (int) Math.floor(Double.parseDouble(lonStr));
                    }
                }
            }
            
            // Try comma-separated format: "longitude,latitude"
            String[] parts = location.split(",");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[0].trim());
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            // Return null if parsing fails
        }
        return null;
    }

    /**
     * Parse latitude from location string
     * Expected format: "longitude,latitude" or {"longitude":X,"latitude":Y}
     * @return latitude as Integer, or null if not found
     */
    public Integer getLatitude() {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try JSON format first
            if (location.contains("\"longitude\"") && location.contains("\"latitude\"")) {
                // Extract latitude using regex-like approach
                String pattern = "\"latitude\":";
                int index = location.indexOf(pattern);
                if (index != -1) {
                    int start = index + pattern.length();
                    // Skip whitespace
                    while (start < location.length() && location.charAt(start) == ' ') {
                        start++;
                    }
                    // Find the end of the number
                    int end = start;
                    while (end < location.length() && 
                           (Character.isDigit(location.charAt(end)) || location.charAt(end) == '-' || location.charAt(end) == '.')) {
                        end++;
                    }
                    if (end > start) {
                        String latStr = location.substring(start, end).trim();
                        return (int) Math.floor(Double.parseDouble(latStr));
                    }
                }
            }
            
            // Try comma-separated format: "longitude,latitude"
            String[] parts = location.split(",");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1].trim());
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            // Return null if parsing fails
        }
        return null;
    }

    /**
     * Set location from longitude and latitude coordinates
     * @param longitude longitude coordinate
     * @param latitude latitude coordinate
     */
    public void setLocationFromCoordinates(Integer longitude, Integer latitude) {
        this.location = longitude + "," + latitude;
    }

    /**
     * Check if video has valid coordinates
     * @return true if both longitude and latitude can be parsed
     */
    public boolean hasValidCoordinates() {
        return getLongitude() != null && getLatitude() != null;
    }
}
