package isa.jutjub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import isa.jutjub.model.Tile;
import isa.jutjub.model.VideoPost;
import isa.jutjub.service.TileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/tiles")
@Tag(name = "Tiles", description = "Map tile management endpoints")
@Slf4j
public class TileController {

    private final TileService tileService;

    @Autowired
    public TileController(TileService tileService) {
        this.tileService = tileService;
    }

    @PostMapping
    @Operation(summary = "Create a new tile", description = "Create a new tile for given coordinates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tile created successfully",
                    content = @Content(schema = @Schema(implementation = Tile.class))),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates or tile already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Tile> createTile(
            @Parameter(description = "Longitude coordinate (integer degree)")
            @RequestParam Integer longitude,
            @Parameter(description = "Latitude coordinate (integer degree)")
            @RequestParam Integer latitude) {

        log.info("Creating tile for coordinates: {}, {}", longitude, latitude);
        try {
            Tile tile = tileService.createTile(longitude, latitude);
            return ResponseEntity.status(HttpStatus.CREATED).body(tile);
        } catch (IllegalArgumentException e) {
            log.error("Failed to create tile: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating tile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{tileId}")
    @Operation(summary = "Get tile by ID", description = "Retrieve a tile by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tile found",
                    content = @Content(schema = @Schema(implementation = Tile.class))),
            @ApiResponse(responseCode = "404", description = "Tile not found")
    })
    public ResponseEntity<Tile> getTile(
            @Parameter(description = "Tile ID")
            @PathVariable Long tileId) {

        log.info("Getting tile: {}", tileId);
        return tileService.getTileById(tileId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/coordinates")
    @Operation(summary = "Get tile by coordinates", description = "Retrieve a tile by longitude and latitude")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tile found",
                    content = @Content(schema = @Schema(implementation = Tile.class))),
            @ApiResponse(responseCode = "404", description = "Tile not found")
    })
    public ResponseEntity<Tile> getTileByCoordinates(
            @Parameter(description = "Longitude coordinate (integer degree)")
            @RequestParam Integer longitude,
            @Parameter(description = "Latitude coordinate (integer degree)")
            @RequestParam Integer latitude) {

        log.info("Getting tile at coordinates: ({}, {})", longitude, latitude);
        return tileService.getTileByCoordinates(longitude, latitude)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{tileId}")
    @Operation(summary = "Delete tile", description = "Delete a tile by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tile deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Tile not found")
    })
    public ResponseEntity<Void> deleteTile(
            @Parameter(description = "Tile ID")
            @PathVariable Long tileId) {

        log.info("Deleting tile: {}", tileId);
        try {
            tileService.deleteTile(tileId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Failed to delete tile: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/videos")
    @Operation(summary = "Add video to tile",
            description = "Automatically add video to appropriate tile based on its coordinates. " +
                    "Rounds coordinates to nearest integer degree and creates tile if it doesn't exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Video added successfully",
                    content = @Content(schema = @Schema(implementation = Tile.class))),
            @ApiResponse(responseCode = "400", description = "Video missing coordinates or invalid data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Tile> addVideoToTile(
            @Parameter(description = "Video to add to tile")
            @RequestBody VideoPost video) {

        log.info("Adding video {} with coordinates ({}, {})",
                video.getId(), video.getLongitude(), video.getLatitude());

        try {
            Tile tile = tileService.addVideoToTile(video);
            return ResponseEntity.ok(tile);
        } catch (IllegalArgumentException e) {
            log.error("Failed to add video: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error adding video: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/videos/{videoId}")
    @Operation(summary = "Add video to tile by ID",
            description = "Load video by ID and add to appropriate tile based on its coordinates. " +
                    "Rounds coordinates to nearest integer degree and creates tile if it doesn't exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Video added successfully",
                    content = @Content(schema = @Schema(implementation = Tile.class))),
            @ApiResponse(responseCode = "404", description = "Video not found"),
            @ApiResponse(responseCode = "400", description = "Video missing coordinates or invalid data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Tile> addVideoToTileById(
            @Parameter(description = "Video ID to load and add to tile")
            @PathVariable Long videoId) {

        log.info("Loading and adding video with ID: {}", videoId);

        try {
            Tile tile = tileService.addVideoToTileById(videoId);
            return ResponseEntity.ok(tile);
        } catch (RuntimeException e) {
            log.error("Video not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error adding video: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{tileId}/videos/{videoId}")
    @Operation(summary = "Remove video from tile", description = "Remove a video from a specific tile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Video removed successfully",
                    content = @Content(schema = @Schema(implementation = Tile.class))),
            @ApiResponse(responseCode = "404", description = "Tile not found"),
            @ApiResponse(responseCode = "400", description = "Invalid video data")
    })
    public ResponseEntity<Tile> removeVideoFromTile(
            @Parameter(description = "Tile ID")
            @PathVariable Long tileId,
            @Parameter(description = "Video ID to remove")
            @PathVariable Long videoId) {

        log.info("Removing video {} from tile {}", videoId, tileId);
        try {
            VideoPost video = new VideoPost();
            video.setId(videoId);

            Tile tile = tileService.removeVideoFromTile(tileId, video);
            return ResponseEntity.ok(tile);
        } catch (RuntimeException e) {
            log.error("Failed to remove video from tile: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error removing video from tile: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{tileId}/videos")
    @Operation(summary = "Get videos in tile", description = "Retrieve all videos in a specific tile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Videos retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Tile not found")
    })
    public ResponseEntity<Set<VideoPost>> getVideosInTile(
            @Parameter(description = "Tile ID")
            @PathVariable Long tileId) {

        log.info("Getting videos for tile: {}", tileId);
        try {
            Tile tile = tileService.getTileById(tileId)
                    .orElseThrow(() -> new RuntimeException("Tile not found: " + tileId));

            return ResponseEntity.ok(tile.getVideos());
        } catch (RuntimeException e) {
            log.error("Failed to get videos from tile: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/bounding-box")
    @Operation(summary = "Get tiles in bounding box",
            description = "Retrieve all tiles within a bounding box defined by min/max longitude and latitude")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tiles retrieved successfully")
    })
    public ResponseEntity<List<Tile>> getTilesInBoundingBox(
            @Parameter(description = "Minimum longitude (integer degree)")
            @RequestParam Integer minLon,
            @Parameter(description = "Minimum latitude (integer degree)")
            @RequestParam Integer minLat,
            @Parameter(description = "Maximum longitude (integer degree)")
            @RequestParam Integer maxLon,
            @Parameter(description = "Maximum latitude (integer degree)")
            @RequestParam Integer maxLat) {

        log.info("Getting tiles in bounding box: ({}, {}) to ({}, {})", minLon, minLat, maxLon, maxLat);
        List<Tile> tiles = tileService.getTilesInBoundingBox(minLon, minLat, maxLon, maxLat);
        return ResponseEntity.ok(tiles);
    }
}