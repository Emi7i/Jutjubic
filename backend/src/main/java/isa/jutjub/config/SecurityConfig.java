package isa.jutjub.config;

import org.springframework.boot.CommandLineRunner;
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.addAllowedOriginPattern("http://localhost:4200");
        configuration.addAllowedOriginPattern("http://127.0.0.1:4200");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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

    /**
     * TEMPORARY: Test BCrypt password encoding on startup
     * Remove this bean after fixing the password issue
     */
    @Bean
    public CommandLineRunner testPasswordEncoder(PasswordEncoder passwordEncoder) {
        return args -> {
            System.out.println("\n╔════════════════════════════════════════════════════════════════════════════╗");
            System.out.println("║                    BCRYPT PASSWORD ENCODER TEST                            ║");
            System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");

            String testPassword = "admin123";
            String oldHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

            // Test the old hash
            boolean oldHashMatches = passwordEncoder.matches(testPassword, oldHash);
            System.out.println("\n[1] Testing OLD hash from database:");
            System.out.println("    Password: " + testPassword);
            System.out.println("    Hash:     " + oldHash);
            System.out.println("    Result:   " + (oldHashMatches ? "✓ MATCHES" : "✗ DOES NOT MATCH"));

            if (!oldHashMatches) {
                System.out.println("\n    ⚠️  WARNING: The hash does NOT match 'admin123'!");
                System.out.println("    This hash might be for a different password.");
            }

            // Generate a new hash
            String newHash = passwordEncoder.encode(testPassword);
            boolean newHashMatches = passwordEncoder.matches(testPassword, newHash);

            System.out.println("\n[2] Generating NEW hash:");
            System.out.println("    Password: " + testPassword);
            System.out.println("    Hash:     " + newHash);
            System.out.println("    Result:   " + (newHashMatches ? "✓ MATCHES" : "✗ DOES NOT MATCH"));

            System.out.println("\n[3] SQL to update admin password:");
            System.out.println("    docker-compose exec postgres psql -U jutjubic_user -d jutjubic_db -c \"UPDATE users SET password = '" + newHash + "' WHERE username = 'admin';\"");

            System.out.println("\n[4] Update your PowerShell seed script:");
            System.out.println("    Change the password line to:");
            System.out.println("    '" + newHash + "',");

            System.out.println("\n╔════════════════════════════════════════════════════════════════════════════╗");
            System.out.println("║  After updating the database, try logging in with: admin / admin123       ║");
            System.out.println("╚════════════════════════════════════════════════════════════════════════════╝\n");
        };
    }
}