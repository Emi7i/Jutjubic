package isa.jutjub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "videos")
@Getter
@Setter
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "isa.jutjub.model.Videos")
public class Videos extends BaseEntity {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Video description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String videoDescription;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags;

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

    // Explicit longitude and latitude
    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "latitude")
    private Double latitude;

    // Derived string version for legacy purposes
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

    // --- Tags helpers ---
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            tags.add(tag.trim().toLowerCase());
        }
    }
    
    public void removeTag(String tag) {
        if (tag != null) {
            tags.remove(tag.trim().toLowerCase());
        }
    }

    // --- Location helpers ---
    public void setCoordinates(Double longitude, Double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.location = (longitude != null && latitude != null) ? longitude + "," + latitude : null;
    }

    public boolean hasValidCoordinates() {
        return longitude != null && latitude != null;
    }
}
