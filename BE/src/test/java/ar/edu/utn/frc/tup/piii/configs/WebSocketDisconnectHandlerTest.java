package ar.edu.utn.frc.tup.piii.configs;

import java.util.List;

import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.MatchStatus;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WebSocketDisconnectHandler}.
 *
 * <p>Manipula el registro estático {@link WebSocketConfig#sessionRegistry} (acceso de paquete)
 * para simular sesiones vivas/caídas y cubre: detección de desconexión por jugador uno/dos,
 * salto de bots, reconexión, y las guardas de oponente null.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketDisconnectHandlerTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketDisconnectHandler handler;

    @BeforeEach
    @AfterEach
    void clearRegistry() {
        WebSocketConfig.sessionRegistry.clear();
    }

    private Match match(String id, String p1, String p2) {
        Match m = new Match();
        m.setId(id);
        m.setPlayerOneId(p1);
        m.setPlayerTwoId(p2);
        m.setStatus(MatchStatus.ACTIVE);
        return m;
    }

    private void markAlive(String playerId) {
        WebSocketConfig.sessionRegistry.put("sess-" + playerId, playerId);
    }

    private void returnMatches(Match... matches) {
        when(matchRepository.findByStatusInOrderByCreatedAtDesc(anyList())).thenReturn(List.of(matches));
    }

    @Test
    void playerOneNotConnected_MarksDisconnectedAndNotifiesOpponent() {
        Match m = match("m1", "p1", "p2");
        markAlive("p2"); // p1 no está vivo
        returnMatches(m);

        handler.checkDisconnectedMatches();

        assertThat(m.getDisconnectedPlayerId()).isEqualTo("p1");
        assertThat(m.getDisconnectedAt()).isNotNull();
        verify(matchRepository).save(m);
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/m1/state/p2"), any(Object.class));
    }

    @Test
    void playerTwoNotConnected_MarksDisconnectedAndNotifiesOpponent() {
        Match m = match("m1", "p1", "p2");
        markAlive("p1"); // p2 no está vivo
        returnMatches(m);

        handler.checkDisconnectedMatches();

        assertThat(m.getDisconnectedPlayerId()).isEqualTo("p2");
        verify(matchRepository).save(m);
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/m1/state/p1"), any(Object.class));
    }

    @Test
    void botPlayer_IsSkipped() {
        Match m = match("m1", "bot-1", "p2");
        markAlive("p2"); // bot no está en el registry pero debe ignorarse igual
        returnMatches(m);

        handler.checkDisconnectedMatches();

        assertThat(m.getDisconnectedPlayerId()).isNull();
        verify(matchRepository, never()).save(any());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void bothConnected_NoAction() {
        Match m = match("m1", "p1", "p2");
        markAlive("p1");
        markAlive("p2");
        returnMatches(m);

        handler.checkDisconnectedMatches();

        verify(matchRepository, never()).save(any());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void disconnectedPlayerReconnects_ClearsFlagAndNotifiesOpponent() {
        Match m = match("m1", "p1", "p2");
        m.setDisconnectedPlayerId("p1");
        markAlive("p1"); // p1 volvió
        markAlive("p2");
        returnMatches(m);

        handler.checkDisconnectedMatches();

        assertThat(m.getDisconnectedPlayerId()).isNull();
        assertThat(m.getDisconnectedAt()).isNull();
        verify(matchRepository).save(m);
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/m1/state/p2"), any(Object.class));
    }

    @Test
    void disconnect_OpponentNull_SavesButDoesNotNotify() {
        Match m = match("m1", "p1", null); // sin oponente
        returnMatches(m);

        handler.checkDisconnectedMatches();

        assertThat(m.getDisconnectedPlayerId()).isEqualTo("p1");
        verify(matchRepository).save(m);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void playerOneIdNull_SkipsPlayerOneCheck() {
        Match m = match("m1", null, "p2"); // sin host
        markAlive("p2");
        returnMatches(m);

        handler.checkDisconnectedMatches();

        assertThat(m.getDisconnectedPlayerId()).isNull();
        verify(matchRepository, never()).save(any());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void playerTwoIdNull_WithPlayerOneAlive_NoAction() {
        Match m = match("m1", "p1", null); // sin invitado
        markAlive("p1");
        returnMatches(m);

        handler.checkDisconnectedMatches();

        assertThat(m.getDisconnectedPlayerId()).isNull();
        verify(matchRepository, never()).save(any());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void playerTwoReconnects_NotifiesPlayerOneAsOpponent() {
        Match m = match("m1", "p1", "p2");
        m.setDisconnectedPlayerId("p2");
        markAlive("p1");
        markAlive("p2"); // p2 volvió
        returnMatches(m);

        handler.checkDisconnectedMatches();

        assertThat(m.getDisconnectedPlayerId()).isNull();
        verify(matchRepository).save(m);
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/m1/state/p1"), any(Object.class));
    }

    @Test
    void reconnect_OpponentNull_ClearsFlagWithoutNotifying() {
        Match m = match("m1", "p1", null);
        m.setDisconnectedPlayerId("p1");
        markAlive("p1");
        returnMatches(m);

        handler.checkDisconnectedMatches();

        assertThat(m.getDisconnectedPlayerId()).isNull();
        verify(matchRepository).save(m);
        verifyNoInteractions(messagingTemplate);
    }
}
