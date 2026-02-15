package isa.jutjub.service;

import isa.jutjub.model.Tile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TileSchedulerService {

    private final TileService tileService;

    @Autowired
    public TileSchedulerService(TileService tileService) {
        this.tileService = tileService;
    }

    /**
     * Periodic redistribution of all videos - every night at 3:00 AM
     * This redistributes all videos to their correct tiles based on coordinates
     * NEVER called by the regular application - only by scheduler
     */
    @Scheduled(cron = "0 0 3 * * ?") // Every day at 3:00 AM
    public void redistributeAllVideosNightly() {
        log.info("Starting nightly video redistribution at {}", LocalDateTime.now());
        
        try {
            int redistributedVideos = tileService.SiftThroughVideos();
            log.info("Nightly video redistribution completed. Redistributed {} videos", redistributedVideos);
        } catch (Exception e) {
            log.error("Error during nightly video redistribution: {}", e.getMessage(), e);
        }
    }

    /**
     * Check and cleanup inactive tiles - weekly
     */
    @Scheduled(cron = "0 0 4 * * SUN") // Every Sunday at 4:00 AM
    public void cleanupInactiveTiles() {
        log.info("Starting weekly inactive tile cleanup at {}", LocalDateTime.now());
        
        try {
            int cleanedTiles = tileService.cleanupInactiveTiles();
            log.info("Weekly tile cleanup completed. Cleaned {} tiles", cleanedTiles);
        } catch (Exception e) {
            log.error("Error during tile cleanup: {}", e.getMessage(), e);
        }
    }
}
