package isa.jutjub.controller;

import isa.jutjub.dto.premiere.PlaybackStateDTO;
import isa.jutjub.service.PremiereSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket controller for real-time premiere synchronization
 *
 * Client subscribes to:
 * - /topic/premiere/{premiereId}/playback - Receives playback state updates
 * - /topic/premiere/{premiereId}/events - Receives premiere lifecycle events
 *
 * Client sends messages to:
 * - /app/premiere/{premiereId}/join - Join premiere as viewer
 * - /app/premiere/{premiereId}/leave - Leave premiere
 * - /app/premiere/{premiereId}/heartbeat - Keep-alive signal
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PremiereWebSocketController {

    private final PremiereSessionService premiereService;

    /**
     * When a client subscribes to playback updates, send them the current state immediately
     */
    @SubscribeMapping("/premiere/{premiereId}/playback")
    public PlaybackStateDTO onSubscribePlayback(@DestinationVariable Long premiereId) {
        log.debug("Client subscribing to premiere {} playback", premiereId);
        return premiereService.getPlaybackState(premiereId);
    }

    /**
     * Handle viewer joining premiere
     */
    @MessageMapping("/premiere/{premiereId}/join")
    public void handleJoin(
            @DestinationVariable Long premiereId,
            @Payload Map<String, Object> payload,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = extractUserId(principal, payload);
        String sessionId = headerAccessor.getSessionId();

        log.info("User {} (session: {}) joining premiere {}", userId, sessionId, premiereId);

        try {
            premiereService.joinPremiere(premiereId, Long.getLong(userId, -1));

            // Store user info in session attributes for disconnect handling
            headerAccessor.getSessionAttributes().put("premiereId", premiereId);
            headerAccessor.getSessionAttributes().put("userId", userId);

        } catch (Exception e) {
            log.error("Error handling join for premiere {}: {}", premiereId, e.getMessage());
        }
    }

    /**
     * Handle viewer leaving premiere
     */
    @MessageMapping("/premiere/{premiereId}/leave")
    public void handleLeave(
            @DestinationVariable Long premiereId,
            @Payload Map<String, Object> payload,
            Principal principal) {

        String userId = extractUserId(principal, payload);
        log.info("User {} leaving premiere {}", userId, premiereId);

        try {
            premiereService.leavePremiere(premiereId, userId);
        } catch (Exception e) {
            log.error("Error handling leave for premiere {}: {}", premiereId, e.getMessage());
        }
    }

    /**
     * Handle heartbeat/keep-alive from clients
     */
    @MessageMapping("/premiere/{premiereId}/heartbeat")
    public void handleHeartbeat(
            @DestinationVariable Long premiereId,
            Principal principal) {

        log.trace("Heartbeat received from {} for premiere {}",
                principal != null ? principal.getName() : "anonymous",
                premiereId);

        // Could extend session timeout or update last-seen timestamp here
    }

    /**
     * Request current playback state (alternative to subscription)
     */
    @MessageMapping("/premiere/{premiereId}/state")
    @SendTo("/topic/premiere/{premiereId}/playback")
    public PlaybackStateDTO requestState(@DestinationVariable Long premiereId) {
        return premiereService.getPlaybackState(premiereId);
    }

    /**
     * Handle chat message from user
     */
    @MessageMapping("/premiere/{premiereId}/chat")
    public void handleChatMessage(
            @DestinationVariable Long premiereId,
            @Payload Map<String, Object> payload,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = extractUserId(principal, payload);
        String message = (String) payload.get("message");
        String username = (String) payload.getOrDefault("username", userId);

        log.info("💬 Chat message from {} in premiere {}: {}", userId, premiereId, message);

        try {
            // Validate message
            if (message == null || message.trim().isEmpty()) {
                log.warn("Empty chat message from user {}", userId);
                return;
            }

            if (message.length() > 500) {
                log.warn("Chat message too long from user {}: {} chars", userId, message.length());
                return;
            }

            // Broadcast chat message
            premiereService.broadcastChatMessage(premiereId, userId, username, message);

        } catch (Exception e) {
            log.error("Error handling chat message for premiere {}: {}", premiereId, e.getMessage());
        }
    }

    // ---- Helper Methods ----

    private String extractUserId(Principal principal, Map<String, Object> payload) {
        // Try to get from principal first (if authenticated)
        if (principal != null) {
            return principal.getName();
        }

        // Fall back to payload
        if (payload != null && payload.containsKey("userId")) {
            return payload.get("userId").toString();
        }

        // Generate anonymous ID if neither available
        return "anonymous-" + System.currentTimeMillis();
    }
}