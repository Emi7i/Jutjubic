package isa.jutjub.dev;

import isa.jutjub.model.VideoPost;
import isa.jutjub.repository.VideoPostRepository;
import isa.jutjub.service.TileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Slf4j
@Profile("kt2")
@RequiredArgsConstructor
public class VideoTestDataSeeder implements CommandLineRunner {

    private final VideoPostRepository videoPostRepository;
    private final TileService tileService;

    @Override
    public void run(String... args) {
        if (videoPostRepository.count() > 0) return;

        String sharedVideoPath = "/videos/sample.mp4";
        String sharedThumbnail = "/thumbnails/sample.png";

        Random rnd = new Random();

        for (int i = 0; i < 5000; i++) {
            VideoPost vp = new VideoPost();
            vp.setTitle("Test video #" + i);
            vp.setVideoDescription("Generated test video");
            vp.setVideoPath(sharedVideoPath);
            vp.setThumbnailPath(sharedThumbnail);
            vp.setVideoFileSize(42_000_000L);

            // scatter them on the map
            int lon = 20 + rnd.nextInt(10);
            int lat = 44 + rnd.nextInt(10);
            vp.setLocationFromCoordinates(lon, lat);

            VideoPost savedPost = videoPostRepository.save(vp);

            if (savedPost.hasValidCoordinates()) {
                try {
                    //log.info("Auto-adding video {} to tiles based on coordinates: {}", savedPost.getId(), savedPost.getLocation());
                    tileService.addVideoToTile(savedPost);
                    //log.info("Successfully added video {} to tiles", savedPost.getId());
                } catch (Exception e) {
                    log.warn("Failed to auto-add video {} to tiles: {}", savedPost.getId(), e.getMessage());
                    // Don't fail the video creation if tile assignment fails
                }
            } else {
                log.info("Video {} has no valid coordinates, skipping tile assignment", savedPost.getId());
            }
        }
    }
}
