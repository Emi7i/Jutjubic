package isa.jutjub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker for broadcasting
        config.enableSimpleBroker("/topic", "/queue");

        // Application destination prefix for messages from clients
        config.setApplicationDestinationPrefixes("/app");

        // User destination prefix for personal messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint for WebSocket connections
        registry.addEndpoint("/ws/premiere")
                .setAllowedOriginPatterns("*") // Configure appropriately for production
                .withSockJS(); // Fallback for browsers that don't support WebSocket

        // Also register without SockJS for native WebSocket clients
        registry.addEndpoint("/ws/premiere")
                .setAllowedOriginPatterns("*");
    }
}