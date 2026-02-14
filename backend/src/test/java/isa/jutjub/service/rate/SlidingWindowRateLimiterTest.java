package isa.jutjub.service.rate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class SlidingWindowRateLimiterTest {

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    private SlidingWindowRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new SlidingWindowRateLimiter();
        rateLimiter.redisTemplate = redisTemplate;
    }

    @Test
    void testTryConsume_Allowed() {
        when(redisTemplate.execute(any(), any(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1L);

        boolean result = rateLimiter.tryConsume("test-key", 1, 10, Duration.ofMinutes(1));

        assertTrue(result);
        verify(redisTemplate).execute(any(), any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testTryConsume_Exceeded() {
        when(redisTemplate.execute(any(), any(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(0L);

        boolean result = rateLimiter.tryConsume("test-key", 1, 10, Duration.ofMinutes(1));

        assertFalse(result);
        verify(redisTemplate).execute(any(), any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testGetCurrentRequestCount() {
        when(redisTemplate.opsForZSet()).thenReturn(mock(org.springframework.data.redis.core.ZSetOperations.class));
        when(redisTemplate.opsForZSet().count(anyString(), anyLong(), anyLong())).thenReturn(5L);

        long count = rateLimiter.getCurrentRequestCount("test-key", 60000, System.currentTimeMillis());

        assertEquals(5, count);
    }

    @Test
    void testResetWindow() {
        rateLimiter.resetWindow("test-key");

        verify(redisTemplate).delete("test-key");
    }

    @Test
    void testTryConsumeWithWeight() {
        when(redisTemplate.execute(any(), any(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1L);

        boolean result = rateLimiter.tryConsume("test-key", 5, 100, Duration.ofMinutes(1));

        assertTrue(result);
    }
}
