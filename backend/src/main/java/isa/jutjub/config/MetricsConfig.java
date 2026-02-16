package isa.jutjub.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom metrics configuration for monitoring application performance
 * Tracks database connections, CPU usage, and active users
 */
@Component
public class MetricsConfig implements HealthIndicator {

    private final MeterRegistry meterRegistry;
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;
    private final OperatingSystemMXBean osBean;

    // Custom metrics
    private Counter activeConnectionsCounter;
    private Counter idleConnectionsCounter;
    private Gauge cpuUsageGauge;
    private AtomicInteger activeUsersCount;
    private Timer requestTimer;

    // Track active users by their last activity timestamp
    private final ConcurrentHashMap<String, Long> activeUsers = new ConcurrentHashMap<>();
    private static final long USER_ACTIVITY_TIMEOUT_MS = 24 * 60 * 60 * 1000; // 24 hours

    @Autowired
    public MetricsConfig(MeterRegistry meterRegistry, DataSource dataSource, EntityManagerFactory entityManagerFactory) {
        this.meterRegistry = meterRegistry;
        this.dataSource = dataSource;
        this.entityManagerFactory = entityManagerFactory;
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
    }

    @PostConstruct
    public void initializeMetrics() {
        // Database connection metrics
        activeConnectionsCounter = Counter.builder("database.connections.active")
                .description("Number of active database connections")
                .tag("application", "jutjubic-backend")
                .register(meterRegistry);

        idleConnectionsCounter = Counter.builder("database.connections.idle")
                .description("Number of idle database connections")
                .tag("application", "jutjubic-backend")
                .register(meterRegistry);

        // CPU usage gauge
        cpuUsageGauge = Gauge.builder("system.cpu.usage", this, MetricsConfig::getCpuUsage)
                .description("Current CPU usage percentage")
                .tag("application", "jutjubic-backend")
                .register(meterRegistry);

        // Active users counter
        activeUsersCount = new AtomicInteger(0);
        Gauge.builder("users.active.count", activeUsersCount, AtomicInteger::get)
                .description("Number of currently active users (24h)")
                .tag("application", "jutjubic-backend")
                .register(meterRegistry);

        // Request processing timer
        requestTimer = Timer.builder("http.server.requests.processing")
                .description("HTTP request processing time")
                .tag("application", "jutjubic-backend")
                .register(meterRegistry);

        // Custom gauge for database connection pool metrics
        Gauge.builder("database.connections.pool.active", this, MetricsConfig::getActiveDbConnections)
                .description("Active database connections from pool")
                .tag("application", "jutjubic-backend")
                .register(meterRegistry);

        Gauge.builder("database.connections.pool.idle", this, MetricsConfig::getIdleDbConnections)
                .description("Idle database connections from pool")
                .tag("application", "jutjubic-backend")
                .register(meterRegistry);

        Gauge.builder("database.connections.pool.max", this, MetricsConfig::getMaxDbConnections)
                .description("Maximum database connections from pool")
                .tag("application", "jutjubic-backend")
                .register(meterRegistry);
    }

    /**
     * Record user activity for tracking active users
     */
    public void recordUserActivity(String userId) {
        activeUsers.put(userId, System.currentTimeMillis());
        cleanupInactiveUsers();
        activeUsersCount.set(activeUsers.size());
    }

    /**
     * Remove inactive users (older than 24 hours)
     */
    private void cleanupInactiveUsers() {
        long currentTime = System.currentTimeMillis();
        activeUsers.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > USER_ACTIVITY_TIMEOUT_MS);
    }

    /**
     * Get current CPU usage percentage
     */
    private double getCpuUsage() {
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100;
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Get number of active database connections
     */
    private double getActiveDbConnections() {
        try {
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                return ((com.zaxxer.hikari.HikariDataSource) dataSource).getHikariPoolMXBean().getActiveConnections();
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Get number of idle database connections
     */
    private double getIdleDbConnections() {
        try {
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                return ((com.zaxxer.hikari.HikariDataSource) dataSource).getHikariPoolMXBean().getIdleConnections();
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Get maximum database connections
     */
    private double getMaxDbConnections() {
        try {
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                return ((com.zaxxer.hikari.HikariDataSource) dataSource).getMaximumPoolSize();
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Record database connection activity
     */
    public void recordActiveConnection() {
        activeConnectionsCounter.increment();
    }

    /**
     * Record idle database connection
     */
    public void recordIdleConnection() {
        idleConnectionsCounter.increment();
    }

    /**
     * Start timer for request processing
     */
    public Timer.Sample startRequestTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop request timer
     */
    public void stopRequestTimer(Timer.Sample sample) {
        sample.stop(requestTimer);
    }

    @Override
    public Health health() {
        try {
            // Check database connectivity
            try (var connection = dataSource.getConnection()) {
                if (!connection.isValid(5)) {
                    return Health.down()
                            .withDetail("database", "Connection validation failed")
                            .build();
                }
            }

            // Check CPU usage
            double cpuUsage = getCpuUsage();
            if (cpuUsage > 90) {
                return Health.down()
                        .withDetail("cpu", "High CPU usage: " + String.format("%.2f%%", cpuUsage))
                        .build();
            }

            // Check database connections
            double activeConnections = getActiveDbConnections();
            double maxConnections = getMaxDbConnections();
            if (maxConnections > 0 && (activeConnections / maxConnections) > 0.9) {
                return Health.down()
                        .withDetail("database", "High connection pool usage: " + 
                                String.format("%.2f%%", (activeConnections / maxConnections) * 100))
                        .build();
            }

            return Health.up()
                    .withDetail("cpu_usage", String.format("%.2f%%", cpuUsage))
                    .withDetail("active_connections", (int) activeConnections)
                    .withDetail("max_connections", (int) maxConnections)
                    .withDetail("active_users", activeUsersCount.get())
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
