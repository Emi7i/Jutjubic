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
import org.springframework.web.cors.CorsConfigurationSource;

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
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                // Angular static assets and API docs
                .requestMatchers("/", "/assets/**", "/static/**", "/favicon.ico", "/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // H2 Console for development
                .requestMatchers("/h2-console/**").permitAll()
                // Error endpoint
                .requestMatchers("/error").permitAll()
                // Public auth endpoints (login, register, activate)
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/activate", "/api/auth/manual-activate").permitAll()
                // Public video viewing endpoints
                .requestMatchers(HttpMethod.GET, "/api/video-posts", "/api/video-posts/*", "/api/video-posts/*/video", "/api/video-posts/*/thumbnail", "/api/video-posts/search", "/api/video-posts/recent", "/api/video-posts/popular", "/api/video-posts/tag/*", "/api/video-posts/*/comments").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)); // For H2 console

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Configure CORS for Angular
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = 
            new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("http://localhost:4200");
        config.addAllowedOriginPattern("http://127.0.0.1:4200");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
