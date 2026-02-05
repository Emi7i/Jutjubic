package isa.jutjub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tiles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"longitude", "latitude"})
})
@Getter
@Setter
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "isa.jutjub.model.Tile")
public class Tile extends BaseEntity {

    @Column(name = "longitude", nullable = false)
    private Integer longitude;

    @Column(name = "latitude", nullable = false)
    private Integer latitude;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tile_videos",
            joinColumns = @JoinColumn(name = "tile_id"),
            inverseJoinColumns = @JoinColumn(name = "video_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<VideoPost> videos = new HashSet<>();

    @Column(name = "video_count")
    private Long videoCount = 0L;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updateVideoCount();
    }

    private void updateVideoCount() {
        this.videoCount = (long) videos.size();
    }

    public void addVideo(VideoPost video) {
        if (video != null) {
            videos.add(video);
            updateVideoCount();
        }
    }

    public void removeVideo(VideoPost video) {
        if (video != null) {
            videos.remove(video);
            updateVideoCount();
        }
    }

    /**
     * Round coordinates to integer degree values
     */
    public static Integer roundCoordinate(Double coordinate) {
        if (coordinate == null) {
            return null;
        }
        return (int) Math.floor(coordinate);
    }

    /**
     * Check if a coordinate falls within this tile's 1-degree grid cell
     * Tile represents area from [longitude, latitude] to [longitude+1, latitude+1]
     */
    public boolean containsCoordinate(Double lon, Double lat) {
        if (lon == null || lat == null) {
            return false;
        }
        return lon >= this.longitude && lon < this.longitude + 1 &&
                lat >= this.latitude && lat < this.latitude + 1;
    }

    /**
     * Check if this tile matches the given integer coordinates
     */
    public boolean matchesCoordinates(Integer lon, Integer lat) {
        return this.longitude.equals(lon) && this.latitude.equals(lat);
    }

    /**
     * Check if this tile overlaps with a bounding box
     * Tile grid: [longitude, latitude] to [longitude+1, latitude+1]
     */
    public boolean overlapsWith(Integer minLon, Integer minLat, Integer maxLon, Integer maxLat) {
        int tileMaxLon = this.longitude + 1;
        int tileMaxLat = this.latitude + 1;

        return !(maxLon <= this.longitude || minLon >= tileMaxLon ||
                maxLat <= this.latitude || minLat >= tileMaxLat);
    }

    /**
     * Check if this tile is completely within a bounding box
     */
    public boolean isWithinBoundingBox(Integer minLon, Integer minLat, Integer maxLon, Integer maxLat) {
        int tileMaxLon = this.longitude + 1;
        int tileMaxLat = this.latitude + 1;

        return this.longitude >= minLon && tileMaxLon <= maxLon &&
                this.latitude >= minLat && tileMaxLat <= maxLat;
    }
}