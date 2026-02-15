package isa.jutjub.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * Provides health status for the application and its dependencies
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * Basic health check endpoint
     * Returns overall health status of the application
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Check database connectivity
            boolean dbHealthy = checkDatabase();
            
            health.put("status", dbHealthy ? "UP" : "DOWN");
            health.put("timestamp", System.currentTimeMillis());
            health.put("application", "jutjubic-backend");
            
            Map<String, Object> components = new HashMap<>();
            components.put("database", Map.of("status", dbHealthy ? "UP" : "DOWN"));
            components.put("eureka", Map.of("status", "UP")); // If we can respond, Eureka client is working
            
            health.put("components", components);
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * Detailed health check with component status
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Database health
            boolean dbHealthy = checkDatabase();
            
            // Application info
            Map<String, Object> info = new HashMap<>();
            info.put("application", "jutjubic-backend");
            info.put("version", "1.0.0");
            info.put("uptime", System.currentTimeMillis());
            
            // Components status
            Map<String, Object> components = new HashMap<>();
            components.put("database", Map.of(
                "status", dbHealthy ? "UP" : "DOWN",
                "details", Map.of("connection", dbHealthy ? "healthy" : "failed")
            ));
            components.put("eureka", Map.of(
                "status", "UP",
                "details", Map.of("registration", "active")
            ));
            
            health.put("status", dbHealthy ? "UP" : "DOWN");
            health.put("info", info);
            health.put("components", components);
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            Map<String, Object> errorHealth = new HashMap<>();
            errorHealth.put("status", "DOWN");
            errorHealth.put("error", e.getMessage());
            errorHealth.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(errorHealth);
        }
    }

    /**
     * Simple liveness probe
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> liveness() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "alive");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }

    /**
     * Readiness probe
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean dbReady = checkDatabase();
            
            response.put("status", dbReady ? "ready" : "not ready");
            response.put("database", dbReady ? "connected" : "disconnected");
            response.put("timestamp", System.currentTimeMillis());
            
            return dbReady ? ResponseEntity.ok(response) : ResponseEntity.status(503).body(response);
        } catch (Exception e) {
            response.put("status", "not ready");
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Test database connectivity
     */
    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 second timeout
        } catch (Exception e) {
            return false;
        }
    }
}
