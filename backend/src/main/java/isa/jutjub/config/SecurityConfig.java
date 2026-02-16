package isa.jutjub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import isa.jutjub.security.JwtAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())  // Disable CORS - gateway handles it
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers(HttpMethod.GET, "/api/video-posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tiles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/premieres/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/premieres").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/test/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/ws/**", "/app/**", "/topic/**", "/queue/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/video-posts/run-etl").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/video-posts/*/simulate-views").permitAll()
                        // Auth endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        // Protected endpoints
                        .requestMatchers(HttpMethod.POST, "/api/video-posts").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/video-posts/*/like").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/video-posts/*/unlike").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/video-posts/*/comments").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/video-posts/*/comments").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/video-posts/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/video-posts/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/video-posts/upload").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
