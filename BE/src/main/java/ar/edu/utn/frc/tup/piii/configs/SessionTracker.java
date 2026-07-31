package ar.edu.utn.frc.tup.piii.configs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * Tracks WebSocket session IDs to player IDs.
 *
 * <p>Uses {@link SessionSubscribeEvent} to capture the mapping when the client
 * subscribes to a topic (session attributes ARE reliably available at this point)
 * and removes the entry on {@link SessionDisconnectEvent}.
 */
@Component
public class SessionTracker {

    private static final Logger log = LoggerFactory.getLogger(SessionTracker.class);

    private final ConcurrentHashMap<String, String> sessionToPlayer = new ConcurrentHashMap<>();

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor == null) {
            log.warn("SessionSubscribeEvent received but headerAccessor is null");
            return;
        }

        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        if (sessionAttrs == null) {
            log.warn("SessionSubscribeEvent: sessionAttrs is null (sessionId={})", headerAccessor.getSessionId());
            return;
        }

        String playerId = (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            log.warn("SessionSubscribeEvent: No PLAYER_ID_ATTR in sessionAttrs={}", sessionAttrs);
            return;
        }

        String wsSessionId = headerAccessor.getSessionId();
        if (wsSessionId != null) {
            sessionToPlayer.put(wsSessionId, playerId);
            log.info("WebSocket tracked: session={} player={}", wsSessionId, playerId);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String wsSessionId = event.getSessionId();
        if (wsSessionId != null) {
            String removed = sessionToPlayer.remove(wsSessionId);
            if (removed != null) {
                log.info("WebSocket disconnected: session={} player={}", wsSessionId, removed);
            } else {
                log.warn("Disconnect event for unknown session: {}", wsSessionId);
            }
        }
    }

    /**
     * Retrieves the player ID associated with a WebSocket session.
     */
    public String getPlayerId(String wsSessionId) {
        return wsSessionId == null ? null : sessionToPlayer.get(wsSessionId);
    }

    /** Returns the number of tracked sessions. */
    public int getSessionCount() {
        return sessionToPlayer.size();
    }
}
