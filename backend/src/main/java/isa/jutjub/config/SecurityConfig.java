package isa.jutjub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(authz -> authz
                        // Angular static assets and API docs
                        .requestMatchers("/", "/assets/**", "/static/**", "/favicon.ico", "/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // H2 Console for development
                        .requestMatchers("/h2-console/**").permitAll()
                        // Error endpoint
                        .requestMatchers("/error").permitAll()
                        // Public auth endpoints (login, register, activate)
                        .requestMatchers("/api/auth/**").permitAll()
                        // Public video endpoints (GET only)
                        .requestMatchers(HttpMethod.GET, "/api/video-posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tiles/**").permitAll()
                        // Public premiere endpoints (GET only)
                        .requestMatchers(HttpMethod.GET, "/api/premieres/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/premieres").permitAll()
                        // Test endpoints
                        .requestMatchers(HttpMethod.GET, "/api/test/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/actuator/health").permitAll()
                        // WebSocket endpoints - MUST be permitted
                        .requestMatchers("/ws/**", "/app/**", "/topic/**", "/queue/**").permitAll()
                        // Authenticated endpoints (temporarily allow upload for testing)
                        .requestMatchers(HttpMethod.POST, "/api/video-posts/upload").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/video-posts").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/video-posts/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/video-posts/**").authenticated()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)); // For H2 console

        return http.build();
    }
}