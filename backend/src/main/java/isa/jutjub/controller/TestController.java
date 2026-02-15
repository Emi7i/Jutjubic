package isa.jutjub.controller;

import com.netflix.appinfo.EurekaInstanceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller for demonstrating load balancing and failover
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Value("${server.port:8080}")
    private int serverPort;

    @Autowired(required = false)
    private EurekaInstanceConfig eurekaInstanceConfig;

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * Simple test endpoint to demonstrate load balancing
     * Returns server information to show which replica is handling the request
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> testLoadBalancing() {
        Map<String, Object> response = new HashMap<>();

        // Get the actual instance ID from Eureka, fallback to environment variable or hostname
        String instanceId = getInstanceId();

        response.put("message", "Hello from backend replica!");
        response.put("serverPort", serverPort);
        response.put("instanceId", instanceId);
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", "healthy");

        // Add some processing time to simulate real work
        try {
            Thread.sleep(50); // 50ms processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Database test endpoint
     * Tests database connectivity and returns basic info
     */
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check if PostgreSQL is accessible
            boolean isDatabaseAvailable = checkDatabaseAvailability();
            
            if (isDatabaseAvailable) {
                response.put("message", "Database connection successful");
                response.put("serverPort", serverPort);
                response.put("instanceId", getContainerName()); // Use container name
                response.put("containerName", getContainerName()); // Container name from Docker
                response.put("eurekaInstanceId", getInstanceId()); // Eureka instance ID
                response.put("database", "connected");
                response.put("queryTime", "2ms");
                response.put("timestamp", System.currentTimeMillis());
            } else {
                response.put("message", "Database not available");
                response.put("serverPort", serverPort);
                response.put("instanceId", getContainerName()); // Use container name
                response.put("containerName", getContainerName()); // Container name from Docker
                response.put("eurekaInstanceId", getInstanceId()); // Eureka instance ID
                response.put("database", "disconnected");
                response.put("timestamp", System.currentTimeMillis());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Database connection failed");
            response.put("error", e.getMessage());
            response.put("serverPort", serverPort);
            response.put("instanceId", getContainerName());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(503).body(response); // Service Unavailable
        }
    }
    
    /**
     * Helper method to get the container name
     * Uses HOSTNAME environment variable set by Docker
     */
    private String getContainerName() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isEmpty()) {
            return hostname;
        }
        
        // Fallback to server port if hostname not available
        return "backend-" + serverPort;
    }

    /**
     * Load test endpoint
     * Simulates some processing load
     */
    @GetMapping("/load")
    public ResponseEntity<Map<String, Object>> loadTest() {
        Map<String, Object> response = new HashMap<>();

        long startTime = System.currentTimeMillis();

        // Simulate some processing
        try {
            Thread.sleep(100 + (int)(Math.random() * 200)); // 100-300ms random processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long processingTime = System.currentTimeMillis() - startTime;

        response.put("message", "Load test completed");
        response.put("serverPort", serverPort);
        response.put("instanceId", getInstanceId());
        response.put("processingTime", processingTime + "ms");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to get the actual instance ID
     * Tries multiple sources in order of preference
     */
    private String getInstanceId() {
        // Try Eureka instance config first
        if (eurekaInstanceConfig != null) {
            return eurekaInstanceConfig.getInstanceId();
        }

        // Fallback to HOSTNAME environment variable (set by Docker)
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isEmpty()) {
            return hostname;
        }

        // Last resort: use server port
        return "backend-" + serverPort;
    }

    /**
     * Check if database is available by attempting to establish a connection
     * @return true if database connection is successful, false otherwise
     */
    private boolean checkDatabaseAvailability() {
        if (dataSource == null) {
            return false;
        }
        
        try (Connection connection = dataSource.getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}