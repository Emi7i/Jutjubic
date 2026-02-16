package com.example.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import com.example.gateway.security.JwtAuthenticationFilter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // Enable and configure CORS
            .authorizeExchange(exchanges -> exchanges
                // WebSocket endpoints
                .pathMatchers("/ws/**").permitAll()
                // Public auth endpoints
                .pathMatchers("/api/auth/**").permitAll()
                // Public video endpoints (GET only)
                .pathMatchers(HttpMethod.GET, "/api/video-posts/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/tiles/**").permitAll()
                // Public premiere endpoints (GET only)
                .pathMatchers(HttpMethod.GET, "/api/premieres/**").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/premieres").permitAll()
                // Test endpoints
                .pathMatchers(HttpMethod.GET, "/api/test/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/actuator/health").permitAll()
                // Likes/unlikes
                .pathMatchers(HttpMethod.POST, "/api/video-posts/*/like").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/video-posts/*/unlike").permitAll()
                // Comments
                .pathMatchers(HttpMethod.GET, "/api/video-posts/*/comments").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/video-posts/*/comments").permitAll()
                // Actuator endpoints for monitoring
                .pathMatchers("/actuator/**").permitAll()
                // WebSocket endpoints - MUST be permitted
                .pathMatchers("/ws/**", "/app/**", "/topic/**", "/queue/**").permitAll()
                // ETL pipeline
                .pathMatchers(HttpMethod.POST, "/api/video-posts/run-etl").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/video-posts/*/simulate-views").permitAll()
                // Permit OPTIONS for CORS preflight
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Authenticated endpoints
                .pathMatchers(HttpMethod.POST, "/api/video-posts").permitAll()
                .pathMatchers(HttpMethod.PUT, "/api/video-posts/**").permitAll()
                .pathMatchers(HttpMethod.DELETE, "/api/video-posts/**").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/video-posts/upload").permitAll()
                // All other endpoints require authentication
                .anyExchange().authenticated()
            )
            .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.addAllowedOriginPattern("http://*:4200");
        configuration.addAllowedHeader("*");
        configuration.addExposedHeader("Authorization");
        configuration.addAllowedMethod("*");
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
