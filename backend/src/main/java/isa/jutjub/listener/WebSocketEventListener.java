package isa.jutjub.listener;

import isa.jutjub.service.PremiereSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listener for WebSocket lifecycle events
 * Handles automatic cleanup when users disconnect
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final PremiereSessionService premiereService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.debug("New WebSocket connection established: {}", sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Retrieve stored session attributes
        Long premiereId = (Long) headerAccessor.getSessionAttributes().get("premiereId");
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");

        if (premiereId != null && userId != null) {
            log.info("User {} disconnected from premiere {} (session: {})",
                    userId, premiereId, sessionId);

            try {
                premiereService.leavePremiere(premiereId, userId);
            } catch (Exception e) {
                log.error("Error handling disconnect for premiere {}: {}",
                        premiereId, e.getMessage());
            }
        } else {
            log.debug("WebSocket disconnected without premiere context: {}", sessionId);
        }
    }
}