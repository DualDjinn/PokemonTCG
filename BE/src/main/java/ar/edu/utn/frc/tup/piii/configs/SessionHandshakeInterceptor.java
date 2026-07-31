package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.services.PlayerService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Copies the authenticated player identity from the HTTP session cookie into
 * WebSocket session attributes during the upgrade handshake.
 *
 * <p>Once stored, STOMP {@code @MessageMapping} handlers can read
 * {@code PLAYER_ID_ATTR} and {@code PLAYER_NAME_ATTR} from
 * {@code SimpMessageHeaderAccessor.getSessionAttributes()} instead of trusting
 * client-supplied identity fields.
 */
public class SessionHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PLAYER_ID_ATTR = "authenticatedPlayerId";
    public static final String PLAYER_NAME_ATTR = "authenticatedPlayerName";

    private final PlayerService playerService;

    public SessionHandshakeInterceptor(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String sessionId = extractSessionId(httpRequest);
            if (sessionId != null) {
                playerService.getPlayerBySession(sessionId).ifPresent(player -> {
                    attributes.put(PLAYER_ID_ATTR, player.getId());
                    attributes.put(PLAYER_NAME_ATTR, player.getTrainerName());
                });
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private String extractSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("sessionId".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
