package ar.edu.utn.frc.tup.piii.configs;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SessionTracker}.
 *
 * <p>Construye eventos STOMP reales ({@link SessionSubscribeEvent}/{@link SessionDisconnectEvent})
 * a partir de un {@link SimpMessageHeaderAccessor} para ejercitar el tracking de sesión → jugador,
 * incluyendo los caminos de guarda (sin attrs, sin playerId, sin sessionId, sesión desconocida).
 */
class SessionTrackerTest {

    private SessionTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new SessionTracker();
    }

    private SessionSubscribeEvent subscribeEvent(String sessionId, Map<String, Object> sessionAttrs) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
        if (sessionId != null) {
            accessor.setSessionId(sessionId);
        }
        if (sessionAttrs != null) {
            accessor.setSessionAttributes(sessionAttrs);
        }
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionSubscribeEvent(this, message);
    }

    private SessionDisconnectEvent disconnectEvent(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        accessor.setSessionId(sessionId);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL);
    }

    private Map<String, Object> attrsWithPlayer(String playerId) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SessionHandshakeInterceptor.PLAYER_ID_ATTR, playerId);
        return attrs;
    }

    @Test
    void onSubscribe_ValidSubscription_TracksSession() {
        tracker.onSubscribe(subscribeEvent("ws-1", attrsWithPlayer("player-1")));

        assertThat(tracker.getPlayerId("ws-1")).isEqualTo("player-1");
        assertThat(tracker.getSessionCount()).isEqualTo(1);
    }

    @Test
    void onSubscribe_NullSessionAttributes_DoesNotTrack() {
        tracker.onSubscribe(subscribeEvent("ws-2", null));

        assertThat(tracker.getSessionCount()).isZero();
    }

    @Test
    void onSubscribe_NoPlayerId_DoesNotTrack() {
        tracker.onSubscribe(subscribeEvent("ws-3", new HashMap<>()));

        assertThat(tracker.getSessionCount()).isZero();
    }

    @Test
    void onSubscribe_NullSessionId_DoesNotTrack() {
        // playerId presente pero sin sessionId: no se puede mapear.
        tracker.onSubscribe(subscribeEvent(null, attrsWithPlayer("player-9")));

        assertThat(tracker.getSessionCount()).isZero();
    }

    @Test
    void onDisconnect_TrackedSession_RemovesIt() {
        tracker.onSubscribe(subscribeEvent("ws-4", attrsWithPlayer("player-4")));
        assertThat(tracker.getSessionCount()).isEqualTo(1);

        tracker.onDisconnect(disconnectEvent("ws-4"));

        assertThat(tracker.getPlayerId("ws-4")).isNull();
        assertThat(tracker.getSessionCount()).isZero();
    }

    @Test
    void onDisconnect_UnknownSession_DoesNothing() {
        assertThatCode(() -> tracker.onDisconnect(disconnectEvent("ws-unknown")))
                .doesNotThrowAnyException();
        assertThat(tracker.getSessionCount()).isZero();
    }

    @Test
    void onDisconnect_NullSessionId_DoesNothing() {
        // El constructor real de SessionDisconnectEvent exige sessionId no-null, así que
        // se mockea el evento para ejercitar la guarda wsSessionId == null.
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn(null);

        assertThatCode(() -> tracker.onDisconnect(event)).doesNotThrowAnyException();
        assertThat(tracker.getSessionCount()).isZero();
    }

    @Test
    void getPlayerId_NullSessionId_ReturnsNull() {
        assertThat(tracker.getPlayerId(null)).isNull();
    }

    @Test
    void getPlayerId_UnknownSession_ReturnsNull() {
        assertThat(tracker.getPlayerId("nope")).isNull();
    }
}
