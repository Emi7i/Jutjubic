package isa.jutjub.config;

import isa.jutjub.model.VideoPost;
import isa.jutjub.model.User;
import isa.jutjub.repository.VideoPostRepository;
import isa.jutjub.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataInitializer {

    private final PasswordEncoder passwordEncoder;

    public DataInitializer(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public CommandLineRunner initData(VideoPostRepository videoPostRepository, UserRepository userRepository) {
        return args -> {
            // Check if data already exists
            if (videoPostRepository.count() == 0) {
                createSampleVideos(videoPostRepository);
            }
            
            // Always create admin user for H2 in-memory database
            createAdminUser(userRepository);
        };
    }

    private void createAdminUser(UserRepository userRepository) {
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@email.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        
        userRepository.save(admin);
        System.out.println("Admin user created: admin / admin123");
    }

    private void createSampleVideos(VideoPostRepository repository) {
        // Sample Video 1
        VideoPost video1 = new VideoPost();
        video1.setTitle("Amazing Nature Documentary");
        video1.setVideoDescription("Explore the breathtaking beauty of nature in this stunning documentary. From majestic mountains to serene oceans, witness the wonders of our planet.");
        video1.setVideoPath("/videos/nature_documentary.mp4");
        video1.setThumbnailPath("/thumbnails/nature_thumb.jpg");
        video1.setVideoFileSize(102400000L); // ~100MB
        video1.setUploadDurationMs(5000L);
        video1.setLocation("Yellowstone National Park, Wyoming");
        video1.setLikesCount(1250L);
        video1.setCommentsCount(89L);
        video1.setViewsCount(5432L);
        
        Set<String> tags1 = new HashSet<>();
        tags1.add("nature");
        tags1.add("documentary");
        tags1.add("wildlife");
        tags1.add("travel");
        video1.setTags(tags1);

        // Sample Video 2
        VideoPost video2 = new VideoPost();
        video2.setTitle("Cooking Masterclass: Italian Pasta");
        video2.setVideoDescription("Learn to make authentic Italian pasta from scratch. Chef Marco shares his family's secret recipe passed down through generations.");
        video2.setVideoPath("/videos/italian_pasta.mp4");
        video2.setThumbnailPath("/thumbnails/pasta_thumb.jpg");
        video2.setVideoFileSize(85000000L); // ~85MB
        video2.setUploadDurationMs(3500L);
        video2.setLocation("Rome, Italy");
        video2.setLikesCount(892L);
        video2.setCommentsCount(156L);
        video2.setViewsCount(3210L);
        
        Set<String> tags2 = new HashSet<>();
        tags2.add("cooking");
        tags2.add("italian");
        tags2.add("pasta");
        tags2.add("recipe");
        video2.setTags(tags2);

        // Sample Video 3
        VideoPost video3 = new VideoPost();
        video3.setTitle("Tech Review: Latest Smartphone");
        video3.setVideoDescription("In-depth review of the latest flagship smartphone. We test the camera, battery life, performance, and more in this comprehensive review.");
        video3.setVideoPath("/videos/smartphone_review.mp4");
        video3.setThumbnailPath("/thumbnails/phone_thumb.jpg");
        video3.setVideoFileSize(120000000L); // ~120MB
        video3.setUploadDurationMs(4200L);
        video3.setLocation("San Francisco, CA");
        video3.setLikesCount(2103L);
        video3.setCommentsCount(234L);
        video3.setViewsCount(8765L);
        
        Set<String> tags3 = new HashSet<>();
        tags3.add("technology");
        tags3.add("smartphone");
        tags3.add("review");
        tags3.add("tech");
        video3.setTags(tags3);

        // Sample Video 4
        VideoPost video4 = new VideoPost();
        video4.setTitle("Yoga for Beginners");
        video4.setVideoDescription("Start your yoga journey with this beginner-friendly session. Perfect for those new to yoga or looking to refresh their practice.");
        video4.setVideoPath("/videos/yoga_beginners.mp4");
        video4.setThumbnailPath("/thumbnails/yoga_thumb.jpg");
        video4.setVideoFileSize(95000000L); // ~95MB
        video4.setUploadDurationMs(2800L);
        video4.setLocation("Bali, Indonesia");
        video4.setLikesCount(567L);
        video4.setCommentsCount(78L);
        video4.setViewsCount(2341L);
        
        Set<String> tags4 = new HashSet<>();
        tags4.add("yoga");
        tags4.add("fitness");
        tags4.add("wellness");
        tags4.add("beginners");
        video4.setTags(tags4);

        // Sample Video 5
        VideoPost video5 = new VideoPost();
        video5.setTitle("Urban Exploration: Hidden City Gems");
        video5.setVideoDescription("Join us as we explore hidden gems in the city. From secret cafes to underground art scenes, discover the urban landscape like never before.");
        video5.setVideoPath("/videos/urban_exploration.mp4");
        video5.setThumbnailPath("/thumbnails/urban_thumb.jpg");
        video5.setVideoFileSize(110000000L); // ~110MB
        video5.setUploadDurationMs(4500L);
        video5.setLocation("New York City, NY");
        video5.setLikesCount(1834L);
        video5.setCommentsCount(198L);
        video5.setViewsCount(6543L);
        
        Set<String> tags5 = new HashSet<>();
        tags5.add("urban");
        tags5.add("exploration");
        tags5.add("travel");
        tags5.add("city");
        video5.setTags(tags5);

        // Save all videos
        repository.save(video1);
        repository.save(video2);
        repository.save(video3);
        repository.save(video4);
        repository.save(video5);

        System.out.println("Sample video data has been initialized!");
    }
}
