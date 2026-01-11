package isa.jutjub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // Swagger / OpenAPI
                        .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // H2 console
                        .requestMatchers("/h2-console/**").permitAll()
                        // API endpoints
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/assets").permitAll()
                        .requestMatchers("/person").permitAll()
                        // Public pages
                        .requestMatchers("/**").permitAll() // HACK: make sure to disable this later and enable only specific pages
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                // For H2 console
                .headers(headers -> headers.frameOptions().disable());

        return http.build();
    }
}