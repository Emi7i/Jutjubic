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
            .authorizeHttpRequests(authz -> authz
                    // Angular static assets and API docs
                    .requestMatchers("/", "/assets/**", "/static/**", "/favicon.ico", "/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    // H2 Console for development
                    .requestMatchers("/h2-console/**").permitAll()
                    // Error endpoint
                    .requestMatchers("/error").permitAll()
                    // Public auth endpoints (login, register, activate)
                    .requestMatchers("/api/auth/**").permitAll()
                    // Public video endpoints
                    .requestMatchers(HttpMethod.GET, "/api/video-posts/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/tiles/**").permitAll()
                    // Test endpoints
                    .requestMatchers(HttpMethod.GET, "/api/test/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/actuator/health").permitAll()
                    // All other endpoints require authentication
                    .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)); // For H2 console

        return http.build();
    }
}
