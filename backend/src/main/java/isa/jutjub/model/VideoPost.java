package isa.jutjub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "video_posts")
@Getter
@Setter
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
}
