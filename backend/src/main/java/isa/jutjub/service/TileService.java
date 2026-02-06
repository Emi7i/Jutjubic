package isa.jutjub.service;

import isa.jutjub.model.Tile;
import isa.jutjub.model.VideoPost;
import isa.jutjub.repository.TileRepository;
import isa.jutjub.repository.VideoPostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class TileService {

    private final TileRepository tileRepository;
    private final VideoPostRepository videoPostRepository;

    @Autowired
    public TileService(TileRepository tileRepository, VideoPostRepository videoPostRepository) {
        this.tileRepository = tileRepository;
        this.videoPostRepository = videoPostRepository;
    }

    /**
     * Get tile by ID
     */
    @Cacheable(value = "tiles", key = "#tileId")
    public Optional<Tile> getTileById(Long tileId) {
        log.debug("Fetching tile by ID: {}", tileId);
        return tileRepository.findById(tileId);
    }

    /**
     * Get tile by coordinates
     */
    @Cacheable(value = "tiles", key = "'coords-' + #longitude + '-' + #latitude")
    public Optional<Tile> getTileByCoordinates(Integer longitude, Integer latitude) {
        log.debug("Fetching tile by coordinates: ({}, {})", longitude, latitude);
        return tileRepository.findByLongitudeAndLatitude(longitude.doubleValue(), latitude.doubleValue());
    }

    /**
     * Get or create tile for given coordinates
     * Automatically creates tile if it doesn't exist
     */
    @CacheEvict(value = "tiles", allEntries = true)
    public Tile getOrCreateTile(Integer longitude, Integer latitude) {
        return tileRepository.findByLongitudeAndLatitude(longitude.doubleValue(), latitude.doubleValue())
                .orElseGet(() -> {
                    Tile newTile = new Tile();
                    newTile.setLongitude(longitude);
                    newTile.setLatitude(latitude);

                    Tile savedTile = tileRepository.save(newTile);
                    log.info("Created new tile with ID: {} for coordinates ({}, {})",
                            savedTile.getId(), longitude, latitude);
                    return savedTile;
                });
    }

    /**
     * Create a tile manually (if needed)
     */
    @CacheEvict(value = "tiles", allEntries = true)
    public Tile createTile(Integer longitude, Integer latitude) {
        if (tileRepository.findByLongitudeAndLatitude(longitude.doubleValue(), latitude.doubleValue()).isPresent()) {
            throw new IllegalArgumentException("Tile already exists for coordinates: " + longitude + ", " + latitude);
        }

        Tile tile = new Tile();
        tile.setLongitude(longitude);
        tile.setLatitude(latitude);

        Tile savedTile = tileRepository.save(tile);
        log.info("Created new tile with ID: {} for coordinates ({}, {})",
                savedTile.getId(), longitude, latitude);

        return savedTile;
    }

    /**
     * Add video to appropriate tile based on its coordinates
     * This is the MAIN method to use when uploading a video
     * Rounds coordinates and finds/creates the appropriate tile
     */
    @CacheEvict(value = "tiles", allEntries = true)
    public Tile addVideoToTile(VideoPost video) {
        if (!video.hasValidCoordinates()) {
            throw new IllegalArgumentException("Video must have valid location coordinates");
        }

        // Round the video coordinates to integers
        Double longitude = video.getLongitude();
        Double latitude = video.getLatitude();

        log.debug("Adding video {} with coordinates ({}, {}) to tile",
                video.getId(), longitude, latitude);

        // Get or create the tile for these rounded coordinates
        Tile tile = getOrCreateTile(longitude.intValue(), latitude.intValue());

        // Add video to tile if not already present
        if (!tile.getVideos().contains(video)) {
            tile.addVideo(video);
            tile = tileRepository.save(tile);
            log.info("Added video {} to tile {} at coordinates ({}, {})",
                    video.getId(), tile.getId(), longitude, latitude);
        } else {
            log.debug("Video {} already exists in tile {}", video.getId(), tile.getId());
        }

        return tile;
    }

    /**
     * Add video to tile by video ID
     * Loads the video from database and adds it to appropriate tile
     */
    @CacheEvict(value = "tiles", allEntries = true)
    public Tile addVideoToTileById(Long videoId) {
        VideoPost video = videoPostRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found with ID: " + videoId));

        log.info("Loaded video {} with coordinates: ({}, {})",
                videoId, video.getLongitude(), video.getLatitude());

        return addVideoToTile(video);
    }

    /**
     * Remove video from tile
     */
    @CacheEvict(value = "tiles", allEntries = true)
    public Tile removeVideoFromTile(Long tileId, VideoPost video) {
        Tile tile = tileRepository.findById(tileId)
                .orElseThrow(() -> new RuntimeException("Tile not found: " + tileId));

        tile.removeVideo(video);
        Tile savedTile = tileRepository.save(tile);

        log.debug("Removed video {} from tile {}", video.getId(), tileId);
        return savedTile;
    }

    /**
     * Remove video from its tile by coordinates
     */
    @CacheEvict(value = "tiles", allEntries = true)
    public Tile removeVideoFromTile(VideoPost video) {
        if (!video.hasValidCoordinates()) {
            throw new IllegalArgumentException("Video must have valid location coordinates");
        }

        Double longitude = video.getLongitude();
        Double latitude = video.getLatitude();

        Tile tile = tileRepository.findByLongitudeAndLatitude(longitude, latitude)
                .orElseThrow(() -> new RuntimeException("Tile not found for video coordinates"));

        tile.removeVideo(video);
        Tile savedTile = tileRepository.save(tile);

        log.info("Removed video {} from tile {} at coordinates ({}, {})",
                video.getId(), tile.getId(), longitude, latitude);
        return savedTile;
    }

    /**
     * Get tiles that overlap with a bounding box
     */
    @Cacheable(value = "tiles", key = "'overlap-' + #minLon + '-' + #minLat + '-' + #maxLon + '-' + #maxLat")
    public List<Tile> getTilesInBoundingBox(Integer minLon, Integer minLat, Integer maxLon, Integer maxLat) {
        log.debug("Fetching tiles in bounding box: ({}, {}) to ({}, {})", minLon, minLat, maxLon, maxLat);

        List<Tile> tiles = new ArrayList<>();

        // Iterate through all grid cells in the bounding box
        for (Integer lon = minLon; lon <= maxLon; lon++) {
            for (Integer lat = minLat; lat <= maxLat; lat++) {
                tileRepository.findByLongitudeAndLatitude(lon.doubleValue(), lat.doubleValue()).ifPresent(tiles::add);
            }
        }

        log.debug("Found {} tiles in bounding box", tiles.size());
        return tiles;
    }

    /**
     * Get tiles with videos (paginated)
     */
    @Cacheable(value = "tiles", key = "'with-videos-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Tile> getTilesWithVideos(Pageable pageable) {
        log.debug("Fetching tiles with videos, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return tileRepository.findTilesWithVideos(pageable);
    }

    /**
     * Delete tile
     */
    @CacheEvict(value = "tiles", allEntries = true)
    public void deleteTile(Long tileId) {
        if (!tileRepository.existsById(tileId)) {
            throw new RuntimeException("Tile not found: " + tileId);
        }

        tileRepository.deleteById(tileId);
        log.info("Deleted tile with ID: {}", tileId);
    }

    /**
     * Get all tiles (paginated)
     */
    @Cacheable(value = "tiles", key = "'all-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Tile> getAllTiles(Pageable pageable) {
        log.debug("Fetching all tiles, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return tileRepository.findAll(pageable);
    }

    /**
     * Count tiles with videos
     */
    @Cacheable(value = "tiles", key = "'count-with-videos'")
    public Long countTilesWithVideos() {
        return tileRepository.countTilesWithVideos();
    }

    /**
     * Sift through all videos and assign them to their correct tiles based on location
     * This function should ONLY be called by the scheduler at 3 AM
     * Never called by the regular application
     */
    @CacheEvict(value = "tiles", allEntries = true)
    public int SiftThroughVideos() {
        log.info("Starting SiftThroughVideos - redistributing all videos to correct tiles");
        
        // Clear all existing tile-video associations
        List<Tile> allTiles = tileRepository.findAll();
        for (Tile tile : allTiles) {
            tile.getVideos().clear();
            tileRepository.save(tile);
        }
        
        // Get all videos and redistribute them to correct tiles
        List<VideoPost> allVideos = videoPostRepository.findAll();
        int redistributedCount = 0;
        
        for (VideoPost video : allVideos) {
            try {
                if (video.hasValidCoordinates()) {
                    // Get or create the correct tile for this video
                    Double longitude = video.getLongitude();
                    Double latitude = video.getLatitude();
                    Tile correctTile = getOrCreateTile(longitude.intValue(), latitude.intValue());
                    
                    // Add video to its correct tile
                    correctTile.addVideo(video);
                    tileRepository.save(correctTile);
                    redistributedCount++;
                    
                    log.debug("Redistributed video {} to tile ({}, {})", 
                            video.getId(), longitude, latitude);
                } else {
                    log.warn("Video {} has invalid coordinates, skipping", video.getId());
                }
            } catch (Exception e) {
                log.error("Error redistributing video {}: {}", video.getId(), e.getMessage());
            }
        }
        
        log.info("Completed SiftThroughVideos. Redistributed {} videos to {} tiles", 
                redistributedCount, allTiles.size());
        return redistributedCount;
    }

    /**
     * Cleanup inactive tiles - used by scheduler for weekly maintenance
     * Goes through each video and reassigns it to the correct tile
     */
    @CacheEvict(value = "tiles", allEntries = true)
    public int cleanupInactiveTiles() {
        log.info("Starting inactive tile cleanup - reassigning all videos");
        
        // Get all videos and reassign them to correct tiles
        List<VideoPost> allVideos = videoPostRepository.findAll();
        int reassignedCount = 0;
        
        for (VideoPost video : allVideos) {
            try {
                if (video.hasValidCoordinates()) {
                    // Get or create the correct tile for this video
                    Double longitude = video.getLongitude();
                    Double latitude = video.getLatitude();
                    Tile correctTile = getOrCreateTile(longitude.intValue(), latitude.intValue());
                    
                    // Add video to its correct tile if not already present
                    if (!correctTile.getVideos().contains(video)) {
                        correctTile.addVideo(video);
                        tileRepository.save(correctTile);
                        reassignedCount++;
                        
                        log.debug("Reassigned video {} to tile ({}, {})", 
                                video.getId(), longitude, latitude);
                    }
                } else {
                    log.warn("Video {} has invalid coordinates, skipping", video.getId());
                }
            } catch (Exception e) {
                log.error("Error reassigning video {}: {}", video.getId(), e.getMessage());
            }
        }
        
        log.info("Completed inactive tile cleanup. Reassigned {} videos", reassignedCount);
        return reassignedCount;
    }
}
