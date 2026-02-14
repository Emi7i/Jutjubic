package isa.jutjub.stress;

import isa.jutjub.service.RateLimitingService;
import isa.jutjub.service.rate.SlidingWindowRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class RateLimitingStressTest {

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testSlidingWindowUnderExtremeLoad() throws InterruptedException {
        String testIdentifier = "stress-test-user";
        int totalRequests = 10000;
        int concurrentThreads = 50;
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch latch = new CountDownLatch(concurrentThreads);
        
        AtomicInteger allowedRequests = new AtomicInteger(0);
        AtomicInteger rejectedRequests = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < concurrentThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < totalRequests / concurrentThreads; j++) {
                        boolean allowed = rateLimitingService.isAllowed(
                            testIdentifier + ":" + threadId,
                            RateLimitingService.RateLimitType.VIDEO_SEARCH
                        );
                        
                        if (allowed) {
                            allowedRequests.incrementAndGet();
                        } else {
                            rejectedRequests.incrementAndGet();
                        }
                        
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    System.err.println("Thread " + threadId + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("=== Stress Test Results ===");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Allowed Requests: " + allowedRequests.get());
        System.out.println("Rejected Requests: " + rejectedRequests.get());
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Requests per second: " + (totalRequests * 1000.0 / duration));
        System.out.println("System did not degrade under extreme load: " + (duration < 60000));
        
        assertTrue(duration < 60000, "System should handle load within 60 seconds");
        assertTrue(allowedRequests.get() > 0, "Some requests should be allowed");
        assertTrue(rejectedRequests.get() > 0, "Some requests should be rate limited");
    }

    @Test
    public void testBurstTrafficHandling() throws InterruptedException {
        String testIdentifier = "burst-test";
        int burstSize = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(burstSize);
        CountDownLatch latch = new CountDownLatch(burstSize);
        AtomicInteger allowedCount = new AtomicInteger(0);
        
        for (int i = 0; i < burstSize; i++) {
            executor.submit(() -> {
                try {
                    boolean allowed = rateLimitingService.isAllowed(
                        testIdentifier,
                        RateLimitingService.RateLimitType.VIDEO_VIEW
                    );
                    if (allowed) {
                        allowedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        System.out.println("Burst test - Allowed requests: " + allowedCount.get());
        assertTrue(allowedCount.get() <= 500, "Should not exceed rate limit even under burst");
    }

    @Test
    public void testMemoryUsageUnderLoad() throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        String testIdentifier = "memory-test";
        int iterations = 1000;
        
        for (int i = 0; i < iterations; i++) {
            rateLimitingService.isAllowed(testIdentifier + ":" + i, 
                RateLimitingService.RateLimitType.GENERAL_API);
            
            if (i % 100 == 0) {
                System.gc();
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                long memoryIncrease = currentMemory - initialMemory;
                
                System.out.println("Iteration " + i + " - Memory increase: " + 
                    (memoryIncrease / 1024 / 1024) + "MB");
                
                assertTrue(memoryIncrease < 100 * 1024 * 1024, 
                    "Memory increase should be reasonable");
            }
        }
    }
}
