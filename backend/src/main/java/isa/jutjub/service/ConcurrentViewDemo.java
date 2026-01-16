package isa.jutjub.service;

import isa.jutjub.model.VideoPost;
import isa.jutjub.repository.VideoPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo script to test concurrent view count increments
 * Run with: --spring.profiles.active=demo --app.demo.concurrent-views=true
 */
@Component
@ConditionalOnProperty(name = "app.demo.concurrent-views", havingValue = "true")
public class ConcurrentViewDemo implements CommandLineRunner {

    @Autowired
    private VideoPostService videoPostService;

    @Autowired
    private VideoPostRepository videoPostRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🎬 CONCURRENT VIEW COUNT DEMONSTRATION");
        System.out.println("=".repeat(60));
        
        // Create a demo video
        VideoPost demoVideo = createDemoVideo();
        
        // Run concurrent view simulation
        simulateConcurrentViews(demoVideo.getId());
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ DEMONSTRATION COMPLETED");
        System.out.println("=".repeat(60));
    }

    private VideoPost createDemoVideo() {
        System.out.println("\n📹 Creating demo video...");
        
        VideoPost video = new VideoPost();
        video.setTitle("Demo Video for Concurrent Views");
        video.setVideoDescription("This video demonstrates thread-safe view counting");
        video.setVideoPath("./uploads/videos/demo_concurrent.mp4");
        video.setViewsCount(0L);
        
        VideoPost saved = videoPostRepository.save(video);
        
        System.out.println("✅ Demo video created with ID: " + saved.getId());
        System.out.println("📊 Initial view count: " + saved.getViewsCount());
        
        return saved;
    }

    private void simulateConcurrentViews(Long videoId) throws Exception {
        final int numberOfThreads = 20;
        final int viewsPerThread = 5;
        final int totalExpectedViews = numberOfThreads * viewsPerThread;
        
        System.out.println("\n🚀 Starting concurrent view simulation...");
        System.out.println("👥 Simulating " + numberOfThreads + " concurrent users");
        System.out.println("👁️  Each user will view the video " + viewsPerThread + " times");
        System.out.println("📈 Total expected views: " + totalExpectedViews);
        
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        AtomicInteger successfulViews = new AtomicInteger(0);
        AtomicInteger failedViews = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // Simulate concurrent users viewing the video
        for (int i = 0; i < numberOfThreads; i++) {
            final int userId = i + 1;
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < viewsPerThread; j++) {
                    try {
                        // Simulate user viewing the video
                        videoPostService.incrementViewCount(videoId);
                        successfulViews.incrementAndGet();
                        
                        // Simulate user watching time (random delay)
                        Thread.sleep(10 + (int)(Math.random() * 50));
                        
                        // Progress indicator
                        if (j == viewsPerThread - 1) {
                            System.out.println("👤 User " + userId + " finished viewing " + viewsPerThread + " times");
                        }
                        
                    } catch (Exception e) {
                        failedViews.incrementAndGet();
                        System.err.println("❌ User " + userId + " view " + (j + 1) + " failed: " + e.getMessage());
                    }
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // Wait for all users to finish
        System.out.println("\n⏳ Waiting for all users to complete...");
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .get(60, TimeUnit.SECONDS);
        
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Get final results
        VideoPost finalVideo = videoPostRepository.findById(videoId).orElse(null);
        long finalViewCount = finalVideo != null ? finalVideo.getViewsCount() : 0;
        
        // Display results
        System.out.println("\n" + "📊 RESULTS");
        System.out.println("-".repeat(40));
        System.out.println("⏱️  Total duration: " + duration + "ms");
        System.out.println("✅ Successful views: " + successfulViews.get());
        System.out.println("❌ Failed views: " + failedViews.get());
        System.out.println("📈 Expected views: " + totalExpectedViews);
        System.out.println("🎯 Actual views: " + finalViewCount);
        System.out.println("📊 Views per second: " + String.format("%.2f", (double) finalViewCount / (duration / 1000.0)));
        
        // Verify correctness
        boolean testPassed = finalViewCount == totalExpectedViews && failedViews.get() == 0;
        
        System.out.println("\n" + (testPassed ? "✅" : "❌") + " TEST " + (testPassed ? "PASSED" : "FAILED"));
        
        if (testPassed) {
            System.out.println("🎉 All concurrent views were counted correctly!");
            System.out.println("🔒 The thread-safe implementation works perfectly!");
        } else {
            System.out.println("⚠️  There were issues with the concurrent view counting.");
        }
        
        System.out.println("\n📹 Demo video final state:");
        System.out.println("   ID: " + videoId);
        System.out.println("   Title: " + (finalVideo != null ? finalVideo.getTitle() : "N/A"));
        System.out.println("   Views: " + finalViewCount);
    }
}
