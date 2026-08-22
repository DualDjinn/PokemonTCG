package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.services.PlayerService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * WebSocket / STOMP configuration for the application.
 *
 * <p>This class configures STOMP-over-WebSocket support and a simple in-memory
 * message broker used by the transport layer to deliver Observer-style updates
 * to connected clients. It registers a STOMP endpoint at "/ws", enables a
 * simple broker on destinations "/topic" and "/queue", sets the application
 * destination prefix to "/app" and the user destination prefix to "/user"
 * (required for user-targeted sends like {@code convertAndSendToUser()}).
 *
 * <p>Design / Pattern note: the backend uses the Observer pattern over STOMP —
 * the engine publishes match update events and transport-layer observers (STOMP
 * subscribers) receive them on topic or user destinations.
 *
 * <p>Origin policy: the handshake is restricted via
 * {@code setAllowedOriginPatterns(allowedOrigins)} to the allowlist configured in
 * {@code app.websocket.allowed-origins} (the same property shared with
 * {@link CorsConfig} for REST). Only those origins may open the WebSocket handshake.
 * This matters because cookie-based authentication is used: restricting origins
 * prevents arbitrary cross-origin pages from attempting handshake/CONNECT with the
 * user's browser cookies. For production, keep this allowlist tight (no wildcard) or
 * switch to token-based auth in the STOMP CONNECT frame.
 *
 * <p>Single responsibility: this class only configures the WebSocket/STOMP broker
 * and endpoints. It does not perform authentication, authorization, or message
 * interception.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    /** Maps WebSocket session ID → player ID. Accessed by {@link WebSocketDisconnectHandler}. */
    static final ConcurrentHashMap<String, String> sessionRegistry = new ConcurrentHashMap<>();

    private final PlayerService playerService;

    @Value("${app.websocket.allowed-origins}")
    private String[] allowedOrigins;

    public WebSocketConfig(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-memory broker for development (suitable for single-instance apps).
        // Heartbeats (10s/10s) keep the WebSocket alive and detect dead connections,
        // important behind a devtunnel that can drop idle connections. Requires a
        // TaskScheduler to send the server-side heartbeats.
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {0, 0})
                .setTaskScheduler(webSocketHeartbeatScheduler());
        // Messages sent to @MessageMapping handlers should be prefixed with /app
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /** Scheduler que el broker usa para emitir los heartbeats STOMP. */
    @Bean
    public TaskScheduler webSocketHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        return scheduler;
    }

     /**
      * Register STOMP endpoint at /ws with permissive origins.
      *
      * NOTE: If Spring Security is added later, permit the /ws handshake endpoint
      * in the security configuration and either disable CSRF for the handshake
      * or switch to token-based authentication (send token in STOMP CONNECT frame).
      */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new SessionHandshakeInterceptor(playerService))
                .setAllowedOriginPatterns(allowedOrigins);
    }

    /**
     * Decorates the STOMP sub-protocol handler to track WebSocket sessions.
     * This is the most reliable way to capture session IDs because we access
     * the WebSocketSession directly (not via STOMP events).
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(new WebSocketHandlerDecoratorFactory() {
            @Override
            public WebSocketHandler decorate(WebSocketHandler handler) {
                return new WebSocketHandlerDecorator(handler) {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                        Map<String, Object> attrs = session.getAttributes();
                        String playerId = attrs == null ? null
                                : (String) attrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
                        if (playerId != null) {
                            sessionRegistry.put(session.getId(), playerId);
                            log.info("WS connected: session={} player={} (registry size={})",
                                    session.getId(), playerId, sessionRegistry.size());
                        } else {
                            log.warn("WS connected with no playerId: session={} attrs={}",
                                    session.getId(), attrs);
                        }
                        super.afterConnectionEstablished(session);
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                        String removed = sessionRegistry.remove(session.getId());
                        if (removed != null) {
                            log.info("WS disconnected: session={} player={} (registry size={})",
                                    session.getId(), removed, sessionRegistry.size());
                        } else {
                            log.warn("WS disconnected (unknown): session={}", session.getId());
                        }
                        super.afterConnectionClosed(session, status);
                    }
                };
            }
        });
    }
}
