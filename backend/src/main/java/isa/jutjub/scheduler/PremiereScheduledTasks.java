package isa.jutjub.scheduler;

import isa.jutjub.service.PremiereSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for premiere automation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PremiereScheduledTasks {

    private final PremiereSessionService premiereService;

    /**
     * Check every 30 seconds for scheduled premieres that should start
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void autoStartScheduledPremieres() {
        log.debug("Checking for scheduled premieres to auto-start");
        try {
            premiereService.autoStartScheduledPremieres();
        } catch (Exception e) {
            log.error("Error in autoStartScheduledPremieres task: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up old finished premieres daily at 2 AM
     * Removes premieres that finished more than 30 days ago
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldPremieres() {
        log.info("Running cleanup task for old premieres");
        try {
            premiereService.cleanupOldPremieres(30);
        } catch (Exception e) {
            log.error("Error in cleanupOldPremieres task: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay =  300000, initialDelay = 10000)
    public void autoFinishLivePremieres() {
        log.debug("Checking for live premieres to finish");
        try {
            premiereService.autoFinishLivePremieres();
        } catch (Exception e) {
            log.error("Error in autoFinishLivePremieres task: {}", e.getMessage(), e);
        }
    }



}